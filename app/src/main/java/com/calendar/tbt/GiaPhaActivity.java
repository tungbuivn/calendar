package com.calendar.tbt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
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
 * Sơ đồ gia phả theo hàng thế hệ: không vẽ đường nối cho rối, mỗi avatar ghi luôn
 * cách xưng hô với người đang làm mốc. Chạm avatar khác là đổi mốc, cả sơ đồ tính lại.
 * Cặp nào chưa nối được qua cha mẹ – vợ chồng thì ghi cách xưng hô nhập tay.
 */
public class GiaPhaActivity extends AppCompatActivity {

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

        list = findViewById(R.id.gia_pha_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new BandAdapter());

        findViewById(R.id.add_person_button).setOnClickListener(v -> openEditor(null));
        findViewById(R.id.relation_button).setOnClickListener(v -> openRelationList());
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
        findViewById(R.id.gia_pha_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
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

    private void openRelationEditor(final List<Person> people, final Relation existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_gia_pha_relation, null);
        final Spinner spinnerA = view.findViewById(R.id.spinner_relation_a);
        final Spinner spinnerB = view.findViewById(R.id.spinner_relation_b);
        final Spinner spinnerGeneration = view.findViewById(R.id.spinner_relation_generation);
        final EditText aCallsB = view.findViewById(R.id.input_a_calls_b);
        final EditText bCallsA = view.findViewById(R.id.input_b_calls_a);
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

        int indexA = existing != null ? ids.indexOf(existing.aId) : ids.indexOf(rootId);
        int indexB = existing != null ? ids.indexOf(existing.bId) : (indexA == 0 ? 1 : 0);
        spinnerA.setSelection(Math.max(0, indexA));
        spinnerB.setSelection(Math.max(0, Math.min(indexB, ids.size() - 1)));
        spinnerGeneration.setSelection(existing == null ? 0 : generationIndex(existing.bGeneration));
        if (existing != null) {
            aCallsB.setText(existing.aCallsB);
            bCallsA.setText(existing.bCallsA);
        }

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
