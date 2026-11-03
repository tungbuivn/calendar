package com.calendar.tbt;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.calendar.tbt.FamilyStore.Person;
import com.calendar.tbt.FamilyStore.Relation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Gia phả vẽ trên canvas: mỗi hàng một thế hệ tính từ người đứng khấn, tên vẽ đủ chữ,
 * có đường nối cha mẹ – con và vợ chồng. Chạm để đổi mốc, chạm hai lần để sửa,
 * giữ lâu kéo ô này thả lên ô khác để nối. Ai không muốn coi sơ đồ thì chuyển sang
 * danh sách theo hàng thế hệ. Cặp chưa nối được qua cha mẹ – vợ chồng thì ghi vai vế
 * nhập tay theo tag.
 */
public class GiaPhaActivity extends AppCompatActivity implements FamilyTreeView.Listener {

    private static final String[] GENERATION_LABELS = {
            "Không rõ bậc",
            "Trên A 2 bậc (như ông bà với cháu)",
            "Trên A 1 bậc (như cha mẹ với con)",
            "Ngang hàng A",
            "Dưới A 1 bậc (như con với cha mẹ)",
            "Dưới A 2 bậc (như cháu với ông bà)",
    };
    private static final int[] GENERATION_VALUES = {
            FamilyStore.GENERATION_UNKNOWN, 2, 1, 0, -1, -2,
    };
    /** Số tag gợi ý một lần gõ — đủ chọn nhanh mà không tràn màn hình. */
    private static final int SUGGEST_LIMIT = 8;

    private static final class Entry {
        final Person person;
        final Kinship.Result result;

        Entry(Person person, Kinship.Result result) {
            this.person = person;
            this.result = result;
        }
    }

    private static final class Band {
        final String title;
        final List<Entry> entries;

        Band(String title, List<Entry> entries) {
            this.title = title;
            this.entries = entries;
        }
    }

    private final List<Band> bands = new ArrayList<>();
    private RecyclerView list;
    private FamilyTreeView tree;
    private TextView viewModeButton;
    private boolean showList;
    private boolean treeEmpty = true;
    private Toolbar toolbar;
    private String rootId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gia_pha);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        toolbar = findViewById(R.id.toolbar);
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            params.topMargin = getResources().getDimensionPixelSize(resourceId);
            toolbar.setLayoutParams(params);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tree = findViewById(R.id.gia_pha_tree);
        tree.setListener(this);
        list = findViewById(R.id.gia_pha_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new BandAdapter());

        findViewById(R.id.add_person_button).setOnClickListener(v -> openEditor(null));
        findViewById(R.id.relation_button).setOnClickListener(v -> openRelationList());
        viewModeButton = findViewById(R.id.view_mode_button);
        viewModeButton.setOnClickListener(v -> {
            showList = !showList;
            applyViewMode();
        });
    }

    /** Sơ đồ cây là mặc định vì tên hiện đủ; danh sách giữ cho màn nhỏ đọc dọc. */
    private void applyViewMode() {
        tree.setVisibility(showList || treeEmpty ? View.GONE : View.VISIBLE);
        list.setVisibility(showList && !treeEmpty ? View.VISIBLE : View.GONE);
        viewModeButton.setText(showList ? "Sơ đồ" : "Danh sách");
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Person> people = FamilyStore.all(this);
        List<Relation> relations = FamilyStore.relations(this);
        rootId = FamilyStore.rootId(this, people);
        bands.clear();

        TreeMap<Integer, List<Entry>> byGeneration =
                new TreeMap<>(Collections.reverseOrder());
        List<Entry> manualOnly = new ArrayList<>();
        List<Entry> unlinked = new ArrayList<>();
        for (Person person : people) {
            Kinship.Result result = Kinship.of(people, relations, rootId, person.id);
            if (result.related && result.generation != FamilyStore.GENERATION_UNKNOWN) {
                List<Entry> row = byGeneration.get(result.generation);
                if (row == null) {
                    row = new ArrayList<>();
                    byGeneration.put(result.generation, row);
                }
                row.add(new Entry(person, result));
            } else if (result.related) {
                manualOnly.add(new Entry(person, result));
            } else {
                unlinked.add(new Entry(person, result));
            }
        }
        // Trong một hàng thì người trên bậc xếp trước.
        for (List<Entry> row : byGeneration.values()) {
            Collections.sort(row, new Comparator<Entry>() {
                @Override
                public int compare(Entry left, Entry right) {
                    return Integer.compare(left.result.seniority, right.result.seniority);
                }
            });
        }
        for (Integer generation : byGeneration.keySet()) {
            List<Entry> row = byGeneration.get(generation);
            bands.add(new Band(bandTitle(generation, row), row));
        }
        if (!manualOnly.isEmpty()) {
            bands.add(new Band("Quan hệ nhập tay", manualOnly));
        }
        if (!unlinked.isEmpty()) {
            bands.add(new Band("Chưa nối gia phả", unlinked));
        }

        boolean empty = people.isEmpty();
        treeEmpty = empty;
        tree.setModel(people, relations, rootId);
        findViewById(R.id.gia_pha_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        applyViewMode();
        toolbar.setSubtitle(empty ? null : "Mốc: " + nameOf(people, rootId));
        list.getAdapter().notifyDataSetChanged();
    }

    private static String bandTitle(int generation, List<Entry> entries) {
        Set<String> terms = new LinkedHashSet<>();
        for (Entry entry : entries) {
            if (entry.result.related && !"self".equals(entry.result.kind)) terms.add(entry.result.term);
        }
        StringBuilder title = new StringBuilder(Kinship.band(generation));
        if (!terms.isEmpty()) {
            title.append(" – ");
            boolean first = true;
            for (String term : terms) {
                if (!first) title.append(", ");
                title.append(term);
                first = false;
            }
        }
        return title.toString();
    }

    private void chooseRoot(Person person) {
        if (person.id.equals(rootId)) return;
        FamilyStore.setRootId(this, person.id);
        reload();
        Toast.makeText(this, "Đã lấy " + person.name + " làm mốc để tính vai vế.", Toast.LENGTH_SHORT).show();
    }

    // ---- FamilyTreeView.Listener ------------------------------------------

    @Override
    public void onPickRoot(Person person) {
        chooseRoot(person);
    }

    @Override
    public void onEditPerson(Person person) {
        openEditor(person);
    }

    /**
     * Kéo A thả lên B: hỏi muốn nối gì, chỉ liệt kê những kiểu còn hợp lệ với dữ liệu
     * hiện có để khỏi phải đoán lần lượt.
     */
    @Override
    public void onLinkPersons(Person source, Person target) {
        final List<Person> people = FamilyStore.all(this);
        final Person from = FamilyStore.find(people, source.id);
        final Person to = FamilyStore.find(people, target.id);
        if (from == null || to == null) {
            toast("Một trong hai người không còn trong gia phả.");
            return;
        }
        final List<String> labels = new ArrayList<>();
        final List<Integer> kinds = new ArrayList<>();
        if (!isCouple(from, to)) {
            labels.add("Vợ / chồng của " + to.name);
            kinds.add(LINK_SPOUSE);
        }
        if (!to.fatherId.equals(from.id)) {
            labels.add(from.name + " là CHA của " + to.name);
            kinds.add(LINK_FATHER_OF);
        }
        if (!to.motherId.equals(from.id)) {
            labels.add(from.name + " là MẸ của " + to.name);
            kinds.add(LINK_MOTHER_OF);
        }
        if (!from.fatherId.equals(to.id) && !from.motherId.equals(to.id)) {
            labels.add(from.name + " là CON của " + to.name);
            kinds.add(LINK_CHILD_OF);
        }
        labels.add("Ghi vai vế nhập tay…");
        kinds.add(LINK_MANUAL);
        new AlertDialog.Builder(this)
                .setTitle(from.name + " → " + to.name)
                .setItems(labels.toArray(new String[0]), (dialog, which) ->
                        applyLink(from, to, kinds.get(which)))
                .setNegativeButton("Bỏ", null)
                .show();
    }

    private static final int LINK_SPOUSE = 0;
    private static final int LINK_FATHER_OF = 1;
    private static final int LINK_MOTHER_OF = 2;
    private static final int LINK_CHILD_OF = 3;
    private static final int LINK_MANUAL = 4;

    /** Nối được ghi qua cùng một cửa với hộp sửa người nên dữ liệu vẫn được kiểm tra lại. */
    private void applyLink(Person from, Person to, int kind) {
        if (kind == LINK_MANUAL) {
            openRelationEditor(FamilyStore.all(this), null, from, to);
            return;
        }
        Person edit = kind == LINK_CHILD_OF ? from : to;
        String done;
        if (kind == LINK_SPOUSE) {
            // saveChecked tự soi chiều ngược nên chỉ cần nối một phía.
            edit.spouseId = from.id;
            done = from.name + " là vợ/chồng của " + to.name;
        } else if (kind == LINK_FATHER_OF) {
            edit.fatherId = from.id;
            done = from.name + " là cha của " + to.name;
        } else if (kind == LINK_MOTHER_OF) {
            edit.motherId = from.id;
            done = from.name + " là mẹ của " + to.name;
        } else {
            // to là cha hay mẹ của from tuỳ giới tính đã khai của to.
            if (to.isFemale()) from.motherId = to.id;
            else from.fatherId = to.id;
            done = from.name + " là con của " + to.name;
        }
        String problem = FamilyStore.saveChecked(this, edit);
        if (problem != null) {
            toast(problem);
            return;
        }
        reload();
        toast("Đã nối: " + done);
    }

    private static boolean isCouple(Person from, Person to) {
        return from.spouseId.equals(to.id) || to.spouseId.equals(from.id);
    }

    private void openEditor(final Person existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_gia_pha_edit, null);
        final EditText nameInput = view.findViewById(R.id.person_name_input);
        final EditText birthInput = view.findViewById(R.id.person_birth_input);
        final RadioGroup genderGroup = view.findViewById(R.id.person_gender);
        final RadioButton male = view.findViewById(R.id.gender_male);
        final RadioButton female = view.findViewById(R.id.gender_female);
        final RadioButton unknown = view.findViewById(R.id.gender_unknown);
        final Spinner father = view.findViewById(R.id.spinner_father);
        final Spinner mother = view.findViewById(R.id.spinner_mother);
        final Spinner spouse = view.findViewById(R.id.spinner_spouse);
        final CheckBox root = view.findViewById(R.id.person_is_root);
        final CheckBox deceased = view.findViewById(R.id.person_is_deceased);

        List<Person> people = FamilyStore.all(this);
        int gender = existing == null ? FamilyStore.MALE : existing.gender;
        (gender == FamilyStore.FEMALE ? female : gender == FamilyStore.UNKNOWN ? unknown : male)
                .setChecked(true);
        if (existing != null) {
            nameInput.setText(existing.name);
            if (existing.birthYear > 0) birthInput.setText(String.valueOf(existing.birthYear));
        }
        root.setChecked(existing == null ? people.isEmpty() : existing.id.equals(rootId));
        deceased.setChecked(existing != null && existing.deceased);
        final List<String> fathers = bindPicker(father, people, existing,
                "— không có cha —", existing == null ? "" : existing.fatherId);
        final List<String> mothers = bindPicker(mother, people, existing,
                "— không có mẹ —", existing == null ? "" : existing.motherId);
        final List<String> spouses = bindPicker(spouse, people, existing,
                "— chưa ghi —", existing == null ? "" : existing.spouseId);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Thêm người" : "Sửa: " + existing.name)
                .setView(view)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast("Hãy nhập tên người.");
                        return;
                    }
                    Person draft = new Person(existing == null ? null : existing.id, name,
                            checkedGender(genderGroup), parseInt(birthInput.getText().toString().trim()),
                            deceased.isChecked(),
                            picked(father, fathers), picked(mother, mothers), picked(spouse, spouses));
                    String problem = FamilyStore.saveChecked(this, draft);
                    if (problem != null) {
                        toast(problem);
                        return;
                    }
                    if (root.isChecked()) FamilyStore.setRootId(this, draft.id);
                    reload();
                })
                .setNegativeButton("Bỏ qua", null);
        if (existing != null) {
            builder.setNeutralButton("Xoá", (dialog, which) -> confirmDelete(existing));
        }
        builder.show();
    }

    /** Vị trí 0 luôn là "không có"; phần còn lại là id những người khác trong gia phả. */
    private List<String> bindPicker(Spinner spinner, List<Person> people, Person existing,
                                    String noneLabel, String currentId) {
        List<String> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        ids.add("");
        labels.add(noneLabel);
        for (Person person : people) {
            if (existing != null && person.id.equals(existing.id)) continue;
            ids.add(person.id);
            labels.add(person.name);
        }
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        int index = ids.indexOf(currentId);
        spinner.setSelection(index < 0 ? 0 : index);
        return ids;
    }

    private static String picked(Spinner spinner, List<String> ids) {
        int index = spinner.getSelectedItemPosition();
        return index < 0 || index >= ids.size() ? "" : ids.get(index);
    }

    private static int checkedGender(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == R.id.gender_female) return FamilyStore.FEMALE;
        return id == R.id.gender_unknown ? FamilyStore.UNKNOWN : FamilyStore.MALE;
    }

    /** Danh sách quan hệ nhập tay: chạm một dòng để sửa, bấm "Thêm" để ghi cặp khác. */
    private void openRelationList() {
        final List<Person> people = FamilyStore.all(this);
        if (people.size() < 2) {
            toast("Cần có ít nhất hai người trong gia phả rồi mới ghi được quan hệ.");
            return;
        }
        final List<Relation> relations = FamilyStore.relations(this);
        List<String> labels = new ArrayList<>();
        for (Relation relation : relations) labels.add(relationSummary(people, relation));
        if (labels.isEmpty()) {
            labels.add("Chưa có quan hệ nhập tay nào.\nBấm \"Thêm\" để ghi cặp đầu tiên.");
        }
        final boolean empty = relations.isEmpty();
        new AlertDialog.Builder(this)
                .setTitle("Quan hệ nhập tay")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (!empty) openRelationEditor(people, relations.get(which));
                })
                .setPositiveButton("Thêm", (dialog, which) -> openRelationEditor(people, null))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private String relationSummary(List<Person> people, Relation relation) {
        Person a = FamilyStore.find(people, relation.aId);
        Person b = FamilyStore.find(people, relation.bId);
        if (a == null || b == null) return "Một trong hai người đã bị xoá — chạm để gỡ quan hệ này.";
        StringBuilder text = new StringBuilder(a.name).append(" ↔ ").append(b.name).append("\n");
        text.append("A gọi B: ").append(relation.aCallsB.isEmpty() ? "—" : relation.aCallsB);
        text.append("   ·   B gọi A: ").append(relation.bCallsA.isEmpty() ? "—" : relation.bCallsA);
        if (Kinship.derivable(people, a.id, b.id)) {
            text.append("\nĐã suy ra được từ gia phả (\"")
                    .append(Kinship.of(people, a.id, b.id).term)
                    .append("\") — bản nhập tay không còn dùng.");
        }
        return text.toString();
    }

    private void openRelationEditor(List<Person> people, Relation existing) {
        openRelationEditor(people, existing, null, null);
    }

    /**
     * @param existing    bản đang sửa, null khi tạo mới
     * @param seedFrom    cặp vừa kéo thả trên sơ đồ, chỉ dùng để chọn sẵn hai người
     * @param seedTo
     */
    private void openRelationEditor(final List<Person> people, final Relation existing,
                                    Person seedFrom, Person seedTo) {
        if (people.size() < 2) {
            toast("Cần có ít nhất hai người trong gia phả rồi mới ghi được quan hệ.");
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_gia_pha_relation, null);
        final Spinner spinnerA = view.findViewById(R.id.spinner_relation_a);
        final Spinner spinnerB = view.findViewById(R.id.spinner_relation_b);
        final Spinner spinnerGeneration = view.findViewById(R.id.spinner_relation_generation);
        final AutoCompleteTextView aCallsB = view.findViewById(R.id.input_a_calls_b);
        final AutoCompleteTextView bCallsA = view.findViewById(R.id.input_b_calls_a);
        final TextView state = view.findViewById(R.id.relation_state);

        final List<String> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Person person : people) {
            ids.add(person.id);
            labels.add(person.name);
        }
        ArrayAdapter<String> personAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels);
        spinnerA.setAdapter(personAdapter);
        spinnerB.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        spinnerGeneration.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Arrays.asList(GENERATION_LABELS)));

        int indexA = Math.max(0, ids.indexOf(existing != null ? existing.aId
                : seedFrom == null ? rootId : seedFrom.id));
        String seedB = existing != null ? existing.bId : seedTo == null ? null : seedTo.id;
        int indexB = seedB == null ? (indexA == 0 ? 1 : 0) : Math.max(0, ids.indexOf(seedB));
        spinnerA.setSelection(indexA);
        spinnerB.setSelection(Math.min(indexB, ids.size() - 1));
        spinnerGeneration.setSelection(existing == null ? 0 : generationIndex(existing.bGeneration));
        if (existing != null) {
            aCallsB.setText(existing.aCallsB);
            bCallsA.setText(existing.bCallsA);
        }

        // Cách gọi là tag chọn từ kho chung: tag nhà đã ghi xếp trước, rồi đến từ vựng sẵn.
        final List<FamilyStore.Tag> storedTags = FamilyStore.tags(this);
        List<String> userTerms = new ArrayList<>();
        for (FamilyStore.Tag tag : storedTags) userTerms.add(tag.term);
        final List<String> vocabulary = KinshipTags.vocabulary(userTerms);
        aCallsB.setAdapter(new TagSuggestAdapter(vocabulary));
        bCallsA.setAdapter(new TagSuggestAdapter(vocabulary));
        aCallsB.setThreshold(1);
        bCallsA.setThreshold(1);
        final boolean[] filling = {false};
        aCallsB.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (filling[0]) return;
            filling[0] = true;
            applyTag(text, bCallsA, spinnerGeneration, 1, storedTags);
            filling[0] = false;
        }));
        bCallsA.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (filling[0]) return;
            filling[0] = true;
            applyTag(text, aCallsB, spinnerGeneration, -1, storedTags);
            filling[0] = false;
        }));

        final List<Relation> relations = FamilyStore.relations(this);
        AdapterView.OnItemSelectedListener watcher = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selected, int position, long id) {
                state.setText(relationState(people, relations, existing,
                        picked(spinnerA, ids), picked(spinnerB, ids)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerA.setOnItemSelectedListener(watcher);
        spinnerB.setOnItemSelectedListener(watcher);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Thêm quan hệ nhập tay" : "Sửa quan hệ nhập tay")
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Bỏ qua", null);
        if (existing != null) {
            builder.setNeutralButton("Xoá", (dialog, which) -> {
                FamilyStore.removeRelation(GiaPhaActivity.this, existing.id);
                reload();
                toast("Đã gỡ quan hệ nhập tay.");
            });
        }
        final AlertDialog dialog = builder.show();
        // Giữ hộp mở khi còn thiếu dữ liệu để không mất phần vừa gõ.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String aId = picked(spinnerA, ids);
            String bId = picked(spinnerB, ids);
            Relation draft = new Relation(existing == null ? null : existing.id, aId, bId,
                    aCallsB.getText().toString().trim(), bCallsA.getText().toString().trim(),
                    GENERATION_VALUES[spinnerGeneration.getSelectedItemPosition()]);
            String problem = FamilyStore.validateRelation(people, relations, draft);
            if (problem == null && Kinship.derivable(people, aId, bId)) {
                Person a = FamilyStore.find(people, aId);
                Person b = FamilyStore.find(people, bId);
                problem = a.name + " và " + b.name + " đã nối được trong gia phả (\""
                        + Kinship.of(people, aId, bId).term + "\") — không cần nhập thêm.";
            }
            if (problem != null) {
                toast(problem);
                return;
            }
            FamilyStore.saveRelation(this, draft);
            reload();
            dialog.dismiss();
        });
    }

    private String relationState(List<Person> people, List<Relation> relations, Relation existing,
                                 String aId, String bId) {
        if (aId.isEmpty() || bId.isEmpty()) return "Hãy chọn đủ hai người.";
        if (aId.equals(bId)) return "Hai phía phải là hai người khác nhau.";
        Person a = FamilyStore.find(people, aId);
        Person b = FamilyStore.find(people, bId);
        if (a == null || b == null) return "Người này không còn trong gia phả.";
        if (Kinship.derivable(people, aId, bId)) {
            return "Đã tính được từ gia phả: " + b.name + " là \""
                    + Kinship.of(people, aId, bId).term + "\" của " + a.name
                    + " — không cần nhập thêm.";
        }
        Relation other = FamilyStore.relationFor(relations, aId, bId,
                existing == null ? "" : existing.id);
        if (other != null) return "Cặp này đã có quan hệ nhập tay — hãy sửa bản đó.";
        return a.name + " và " + b.name + " chưa nối được qua cha mẹ – vợ chồng. Hãy nhập cách xưng hô.";
    }

    private static int generationIndex(int value) {
        for (int i = 0; i < GENERATION_VALUES.length; i++) {
            if (GENERATION_VALUES[i] == value) return i;
        }
        return 0;
    }

    /**
     * Tag đã có thì điền luôn chiều ngược lại và bậc: A gọi B là "chú" thì B gọi lại "cháu".
     * Ô kia chỉ được điền khi còn trống, người dùng vẫn sửa lại được.
     *
     * @param sign +1 khi tag nằm ở chiều "A gọi B", -1 khi ở chiều "B gọi A"
     */
    private void applyTag(String term, AutoCompleteTextView otherSide, Spinner generation, int sign,
                          List<FamilyStore.Tag> storedTags) {
        FamilyStore.Tag custom = FamilyStore.findTag(storedTags, term);
        String reciprocal = custom != null && !custom.reciprocal.isEmpty()
                ? custom.reciprocal : KinshipTags.reciprocalOf(term);
        if (reciprocal.isEmpty() && custom == null) return;
        if (otherSide.getText().toString().trim().isEmpty() && !reciprocal.isEmpty()) {
            otherSide.setText(reciprocal);
        }
        int value = custom != null ? sign * custom.generation : sign * KinshipTags.generationOf(term);
        for (int i = 0; i < GENERATION_VALUES.length; i++) {
            if (GENERATION_VALUES[i] == value) {
                generation.setSelection(i);
                return;
            }
        }
    }

    private interface TermListener {
        void onTerm(String term);
    }

    /** Ô tag chỉ cần biết người dùng vừa gõ/chọn xong gì. */
    private static final class SimpleTextWatcher implements TextWatcher {
        private final TermListener listener;

        SimpleTextWatcher(TermListener listener) {
            this.listener = listener;
        }

        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable text) {
            listener.onTerm(text == null ? "" : text.toString().trim());
        }
    }

    /** Gợi ý tag khi gõ: khớp cả tag chứa chuỗi đang gõ, lẫn chuỗi đang gõ chứa tag. */
    private final class TagSuggestAdapter extends ArrayAdapter<String> {
        private final List<String> vocabulary;
        // Một Filter duy nhất: tạo mới mỗi lần gõ thì kết quả cũ có thể về sau kết quả mới.
        private final Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<String> matches = KinshipTags.suggest(
                        String.valueOf(constraint), vocabulary, SUGGEST_LIMIT);
                FilterResults results = new FilterResults();
                results.values = matches;
                results.count = matches.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        };

        TagSuggestAdapter(List<String> vocabulary) {
            super(GiaPhaActivity.this, android.R.layout.simple_dropdown_item_1line);
            this.vocabulary = vocabulary;
        }

        @Override
        public Filter getFilter() {
            return filter;
        }
    }

    private void confirmDelete(final Person person) {
        new AlertDialog.Builder(this)
                .setTitle("Xoá người này")
                .setMessage("Xoá " + person.name + " khỏi gia phả? Các mối nối tham chiếu tới người này cũng bị gỡ.")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    FamilyStore.remove(this, person.id);
                    reload();
                })
                .setNegativeButton("Giữ lại", null)
                .show();
    }

    private static int parseInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value < 1000 || value > 2300 ? 0 : value;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private final class BandAdapter extends RecyclerView.Adapter<BandHolder> {

        @NonNull
        @Override
        public BandHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BandHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gia_pha_band, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BandHolder holder, int position) {
            holder.bind(bands.get(position));
        }

        @Override
        public int getItemCount() {
            return bands.size();
        }
    }

    private final class BandHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final FlowLayout people;

        BandHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.band_title);
            people = itemView.findViewById(R.id.band_people);
        }

        void bind(Band band) {
            title.setText(band.title);
            people.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            for (Entry entry : band.entries) {
                View chip = inflater.inflate(R.layout.item_gia_pha_person, people, false);
                bindChip(chip, entry);
                people.addView(chip);
            }
        }
    }

    private void bindChip(View chip, Entry entry) {
        TextView avatar = chip.findViewById(R.id.person_avatar);
        TextView name = chip.findViewById(R.id.person_name);
        TextView term = chip.findViewById(R.id.person_term);
        Person person = entry.person;
        avatar.setText(person.initial());
        avatar.setBackgroundResource(avatarBackground(person));
        chip.setBackgroundResource(person.deceased
                ? R.drawable.border_background_deceased : R.drawable.border_background);
        name.setText(person.name);
        name.setTextColor(ContextCompat.getColor(this,
                person.deceased ? R.color.deceasedText : R.color.textPrimary));
        term.setText(entry.result.term);
        term.setTextColor(ContextCompat.getColor(this,
                person.id.equals(rootId) ? R.color.primaryDarkColor : R.color.secondaryColor));
        chip.setOnClickListener(v -> chooseRoot(person));
        chip.setOnLongClickListener(v -> {
            openEditor(person);
            return true;
        });
    }

    private int avatarBackground(Person person) {
        boolean root = person.id.equals(rootId);
        if (person.deceased) return root ? R.drawable.family_avatar_deceased_self
                : R.drawable.family_avatar_deceased;
        if (root) return R.drawable.family_avatar_self;
        if (person.isMale()) return R.drawable.family_avatar_male;
        return person.isFemale() ? R.drawable.family_avatar_female
                : R.drawable.family_avatar_unknown;
    }

    private static String nameOf(List<Person> people, String id) {
        Person person = FamilyStore.find(people, id);
        return person == null ? "—" : person.name;
    }
}
