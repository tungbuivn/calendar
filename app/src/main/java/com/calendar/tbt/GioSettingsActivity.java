package com.calendar.tbt;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Cài đặt ngày giỗ: nhập ngày dương hay âm đều quy đổi sang âm lịch để tính lễ. */
public class GioSettingsActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 3001;

    private final List<GioStore.Memorial> items = new ArrayList<>();
    private final List<Integer> years = new ArrayList<>();
    private final List<FamilyStore.Person> family = new ArrayList<>();
    private final List<FamilyStore.Relation> familyRelations = new ArrayList<>();
    private String familyRoot = "";
    private RecyclerView list;
    private int currentYear;
    private SwitchMaterial reminderEnabledSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gio_settings);
        AppChrome.setup(this, R.id.today);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        Toolbar toolbar = findViewById(R.id.toolbar);
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            params.topMargin = getResources().getDimensionPixelSize(resourceId);
            toolbar.setLayoutParams(params);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = currentYear + 1; year >= 1900; year--) years.add(year);

        setupReminderDays();
        setupReminderEnabledSwitch();

        list = findViewById(R.id.gio_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new GioAdapter());

        findViewById(R.id.add_gio_button).setOnClickListener(v -> openEditor(null));
    }

    /**
     * Công tắc bật/tắt thông báo. Chỉ xin quyền POST_NOTIFICATIONS đúng lúc người dùng
     * bật tính năng, thay vì hỏi ngay khi mở app.
     */
    private void setupReminderEnabledSwitch() {
        reminderEnabledSwitch = findViewById(R.id.reminder_enabled_switch);
        // Người dùng đã cấp quyền từ trước thì coi như đang bật, tránh mất thông báo
        // sau khi cập nhật ứng dụng.
        if (hasNotificationPermission() && !ReminderPreferences.areNotificationsEnabled(this)) {
            ReminderPreferences.setNotificationsEnabled(this, true);
        }
        reminderEnabledSwitch.setChecked(ReminderPreferences.areNotificationsEnabled(this));
        reminderEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (hasNotificationPermission()) {
                    enableReminders();
                } else if (canAskForPermissionAgain()) {
                    // Chưa hỏi lần nào, hoặc người dùng từ chối nhưng chưa chọn
                    // "không hỏi lại": vẫn hiện được hộp thoại xin quyền.
                    requestNotificationPermission();
                } else {
                    // Đã chọn "không hỏi lại": chỉ có thể mở cài đặt hệ thống.
                    reminderEnabledSwitch.setChecked(false);
                    Toast.makeText(this, "Bạn đã chặn thông báo. Hãy mở cài đặt ứng dụng để cấp quyền.",
                            Toast.LENGTH_LONG).show();
                    openAppNotificationSettings();
                }
            } else {
                ReminderPreferences.setNotificationsEnabled(this, false);
                PrayerReminder.cancelAll(this);
            }
        });
    }

    /**
     * Có thể hiện hộp thoại xin quyền hay không. Chỉ khi đã hỏi rồi mà rationale vẫn false
     * thì mới chắc chắn là người dùng đã chọn "không hỏi lại".
     */
    private boolean canAskForPermissionAgain() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.POST_NOTIFICATIONS)) {
            return true;
        }
        return !ReminderPreferences.wasPermissionRequested(this);
    }

    /** Mở trang cài đặt ứng dụng để người dùng cấp lại quyền gửi thông báo. */
    private void openAppNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
            startActivity(fallback);
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            enableReminders();
            return;
        }
        ReminderPreferences.setPermissionRequested(this);
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.POST_NOTIFICATIONS },
                NOTIFICATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST_CODE) return;

        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            enableReminders();
        } else {
            // Người dùng từ chối: trả công tắc về trạng thái tắt.
            reminderEnabledSwitch.setChecked(false);
            ReminderPreferences.setNotificationsEnabled(this, false);
            new AlertDialog.Builder(this)
                    .setTitle("Chưa bật thông báo")
                    .setMessage("Bạn cần cấp quyền gửi thông báo để nhận nhắc trước sự kiện.")
                    .setPositiveButton("Mở cài đặt", (dialog, which) -> openAppNotificationSettings())
                    .setNegativeButton("Để sau", null)
                    .show();
        }
    }

    private void enableReminders() {
        ReminderPreferences.setNotificationsEnabled(this, true);
        PrayerReminder.refreshNow(this);
    }

    private void setupReminderDays() {
        Spinner spinner = findViewById(R.id.reminder_days_spinner);
        String[] labels = new String[ReminderPreferences.MAX_DAYS + 1];
        for (int i = 0; i <= ReminderPreferences.MAX_DAYS; i++) {
            labels[i] = i + " ngày";
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int saved = ReminderPreferences.getDaysBefore(this);
        spinner.setSelection(saved, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == ReminderPreferences.getDaysBefore(GioSettingsActivity.this)) return;
                ReminderPreferences.setDaysBefore(GioSettingsActivity.this, position);
                PrayerReminder.refreshNow(GioSettingsActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        GioStore.ensureFamilyLinks(this);
        items.clear();
        items.addAll(GioStore.all(this));
        family.clear();
        family.addAll(FamilyStore.all(this));
        familyRelations.clear();
        familyRelations.addAll(FamilyStore.relations(this));
        familyRoot = FamilyStore.rootId(this, family);
        findViewById(R.id.gio_empty).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        list.getAdapter().notifyDataSetChanged();
    }

    private void openEditor(final GioStore.Memorial existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_gio_edit, null);
        final EditText nameInput = view.findViewById(R.id.gio_name_input);
        final EditText burialInput = view.findViewById(R.id.gio_burial_input);
        final Spinner personSpinner = view.findViewById(R.id.spinner_gio_person);
        final CheckBox addToFamily = view.findViewById(R.id.gio_add_to_family);
        final TextView relationNote = view.findViewById(R.id.gio_relation_note);
        final RadioButton solarMode = view.findViewById(R.id.mode_solar);
        final RadioButton lunarMode = view.findViewById(R.id.mode_lunar);
        final Spinner daySpinner = view.findViewById(R.id.spinner_day);
        final Spinner monthSpinner = view.findViewById(R.id.spinner_month);
        final Spinner yearSpinner = view.findViewById(R.id.spinner_year);
        final CheckBox leap = view.findViewById(R.id.gio_leap);
        final TextView conversion = view.findViewById(R.id.gio_convert);

        // Giỗ tính theo âm lịch nên khi sửa mặc định mở đúng ngày âm đã lưu.
        boolean lunar = existing != null;
        solarMode.setChecked(!lunar);
        lunarMode.setChecked(lunar);
        leap.setChecked(existing != null && existing.lunarLeap);
        if (existing != null) {
            nameInput.setText(existing.name);
            burialInput.setText(existing.burialPlace);
        }

        final List<String> personIds = new ArrayList<>();
        List<String> personLabels = new ArrayList<>();
        personIds.add("");
        personLabels.add("— chưa nối gia phả —");
        for (FamilyStore.Person person : family) {
            personIds.add(person.id);
            personLabels.add(person.name + (person.deceased ? " (đã khuất)" : ""));
        }
        personSpinner.setAdapter(adapter(personLabels));
        final String[] linked = {existing == null ? "" : existing.personId};
        pick(personSpinner, Math.max(0, personIds.indexOf(linked[0])));
        personSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View child, int position, long id) {
                linked[0] = position <= 0 || position >= personIds.size() ? "" : personIds.get(position);
                addToFamily.setVisibility(linked[0].isEmpty() ? View.VISIBLE : View.GONE);
                showRelationNote(relationNote, linked[0]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Trùng tên trong gia phả thì nối luôn để bài khấn có vai vế, người dùng vẫn đổi được.
        nameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!linked[0].isEmpty()) return;
                FamilyStore.Person sameName = FamilyStore.byName(family, s.toString().trim());
                if (sameName != null) pick(personSpinner, personIds.indexOf(sameName.id));
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        addToFamily.setText("Tự động thêm người này vào Gia phả");
        addToFamily.setChecked(true);
        addToFamily.setEnabled(false);
        addToFamily.setVisibility(linked[0].isEmpty() ? View.VISIBLE : View.GONE);
        showRelationNote(relationNote, linked[0]);
        int day = existing == null ? Calendar.getInstance().get(Calendar.DAY_OF_MONTH) : existing.lunarDay;
        int month = existing == null ? Calendar.getInstance().get(Calendar.MONTH) + 1 : existing.lunarMonth;
        int year = existing == null ? currentYear : existing.lunarYear;

        bindSpinners(daySpinner, monthSpinner, yearSpinner, day, month, year, lunar, leap);
        final int[] chosen = {day, month, year};
        final boolean[] binding = {false};

        AdapterView.OnItemSelectedListener sync = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View child, int position, long id) {
                if (binding[0]) return;
                if (parent == daySpinner) chosen[0] = position + 1;
                else if (parent == monthSpinner) chosen[1] = position + 1;
                else chosen[2] = yearAt(position);
                showConversion(conversion, solarMode.isChecked(), chosen[0], chosen[1], chosen[2],
                        leap.isChecked());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        daySpinner.setOnItemSelectedListener(sync);
        monthSpinner.setOnItemSelectedListener(sync);
        yearSpinner.setOnItemSelectedListener(sync);

        ((android.widget.RadioGroup) view.findViewById(R.id.gio_mode))
                .setOnCheckedChangeListener((group, checkedId) -> {
                    boolean toLunar = lunarMode.isChecked();
                    // Giữ nguyên ngày đang chọn theo lịch mới, không đọc lại con số cũ thành lịch khác.
                    int[] carried = carryDate(chosen[0], chosen[1], chosen[2], toLunar, leap.isChecked());
                    chosen[0] = carried[0];
                    chosen[1] = carried[1];
                    chosen[2] = carried[2];
                    binding[0] = true;
                    bindSpinners(daySpinner, monthSpinner, yearSpinner, chosen[0], chosen[1], chosen[2],
                            toLunar, leap);
                    binding[0] = false;
                    showConversion(conversion, !toLunar, chosen[0], chosen[1], chosen[2], leap.isChecked());
                });
        leap.setOnCheckedChangeListener((button, checked) -> showConversion(conversion,
                solarMode.isChecked(), chosen[0], chosen[1], chosen[2], checked));

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Thêm ngày giỗ" : "Sửa ngày giỗ")
                .setView(view)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast("Hãy nhập tên người đã khuất.");
                        return;
                    }
                    String burial = burialInput.getText().toString();
                    String id = existing == null ? null : existing.id;
                    String personId = linked[0];
                    FamilyStore.Person linkedPerson = personId.isEmpty()
                            ? null : FamilyStore.find(GioSettingsActivity.this, personId);
                    if (linkedPerson == null) {
                        // Người chưa có trong gia phả: tạo luôn để có thể sửa trong cây.
                        FamilyStore.Person created = new FamilyStore.Person(null, name,
                                FamilyStore.UNKNOWN, 0, true, "", "", "");
                        String problem = FamilyStore.saveChecked(GioSettingsActivity.this, created);
                        if (problem != null) {
                            toast(problem);
                            return;
                        }
                        personId = created.id;
                    } else {
                        // Sửa tên ở ngày giỗ thì cập nhật luôn người trong gia phả.
                        boolean changed = !linkedPerson.name.equals(name) || !linkedPerson.deceased;
                        linkedPerson.name = name;
                        linkedPerson.deceased = true;
                        if (changed) FamilyStore.save(GioSettingsActivity.this, linkedPerson);
                    }
                    GioStore.Memorial saved;
                    try {
                        saved = solarMode.isChecked()
                                ? GioStore.fromSolar(id, name, chosen[0], chosen[1], chosen[2],
                                burial, personId)
                                : GioStore.fromLunar(id, name, chosen[0], chosen[1], chosen[2],
                                leap.isChecked(), burial, personId);
                    } catch (IllegalArgumentException e) {
                        toast("Ngày này không quy đổi được (app tính từ năm 1900).");
                        return;
                    }
                    if (saved == null) {
                        toast("Năm đã chọn không có tháng nhuận đó.");
                        return;
                    }
                    markDeceased(personId);
                    GioStore.save(GioSettingsActivity.this, saved);
                    reload();
                    PrayerReminder.notifyToday(GioSettingsActivity.this);
                })
                .setNegativeButton("Bỏ qua", null)
                .show();
    }

    /** Vai vế bài khấn sẽ dùng — tính từ người đứng khấn đã chọn trong gia phả. */
    private void showRelationNote(TextView note, String personId) {
        if (family.isEmpty()) {
            note.setText("Chưa có gia phả nên bài khấn để trống vai vế. Vào mục Gia phả để thêm người đứng khấn trước.");
            return;
        }
        if (personId.isEmpty()) {
            note.setText("Chưa nối gia phả nên bài khấn để trống vai vế của người được khấn.");
            return;
        }
        String term = Kinship.prayerTerm(family, familyRelations, familyRoot, personId);
        if (term.isEmpty()) {
            note.setText("Gia phả chưa nối được người này với người đứng khấn — vào Gia phả › Quan hệ nhập tay để ghi.");
            return;
        }
        FamilyStore.Person root = FamilyStore.find(family, familyRoot);
        note.setText("Khi khấn, " + (root == null ? "người đứng khấn" : root.name)
                + " gọi người này là \"" + term + "\".");
    }

    /** Người có ngày giỗ là người đã khuất: đồng bộ nền xám trong sơ đồ gia phả. */
    private void markDeceased(String personId) {
        FamilyStore.Person person = FamilyStore.find(family, personId);
        if (person != null && !person.deceased) {
            person.deceased = true;
            FamilyStore.save(this, person);
        }
    }

    /** Số ngày trong tháng phụ thuộc lịch đang nhập: âm lịch tối đa 30, dương lịch 31. */
    private void bindSpinners(Spinner day, Spinner month, Spinner year,
                              int chosenDay, int chosenMonth, int chosenYear, boolean lunar, CheckBox leap) {
        int maxDay = lunar ? 30 : 31;
        List<String> dayLabels = new ArrayList<>();
        for (int d = 1; d <= maxDay; d++) dayLabels.add(String.valueOf(d));
        List<String> monthLabels = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            monthLabels.add(lunar ? "Tháng " + LunarNames.month(m, false) : "Tháng " + m);
        }
        List<String> yearLabels = new ArrayList<>();
        for (Integer value : years) yearLabels.add(String.valueOf(value));

        day.setAdapter(adapter(dayLabels));
        month.setAdapter(adapter(monthLabels));
        year.setAdapter(adapter(yearLabels));
        leap.setVisibility(lunar ? View.VISIBLE : View.GONE);

        pick(day, Math.min(chosenDay, maxDay) - 1);
        pick(month, chosenMonth - 1);
        int yearIndex = years.indexOf(chosenYear);
        pick(year, yearIndex < 0 ? 0 : yearIndex);
    }

    /** Đổi ngày đang chọn sang lịch ngược lại; ngày không tồn tại thì giữ nguyên số. */
    private int[] carryDate(int day, int month, int year, boolean toLunar, boolean leap) {
        try {
            if (toLunar) {
                LunarCalendar.LunarDate lunar = LunarCalendar.getLunarDate(day, month, year);
                return new int[]{lunar.day, lunar.month, lunar.year};
            }
            int[] solar = LunarCalendar.convertLunar2Solar(day, month, year, leap ? 1 : 0, 7.0);
            if (solar[0] != 0) return solar;
        } catch (IllegalArgumentException ignored) {
        }
        return new int[]{day, month, year};
    }

    private void showConversion(TextView conversion, boolean solar, int day, int month, int year, boolean leap) {
        try {
            if (solar) {
                LunarCalendar.LunarDate lunar = LunarCalendar.getLunarDate(day, month, year);
                conversion.setText(String.format("Dương lịch %d/%d/%d → âm lịch %d/%d, năm %s%s",
                        day, month, year, lunar.day, lunar.month, lunar.getYearName(),
                        lunar.isLeap ? " (nhuận)" : ""));
            } else {
                int[] solarDate = LunarCalendar.convertLunar2Solar(day, month, year, leap ? 1 : 0, 7.0);
                if (solarDate[0] == 0) {
                    conversion.setText("Tháng nhuận này không tồn tại trong năm đã chọn.");
                } else {
                    conversion.setText(String.format("Âm lịch %d/%d/%d%s → dương lịch %d/%d/%d",
                            day, month, year, leap ? " nhuận" : "",
                            solarDate[0], solarDate[1], solarDate[2]));
                }
            }
        } catch (IllegalArgumentException e) {
            conversion.setText("Ngoài phạm vi quy đổi (app tính từ năm 1900).");
        }
    }

    private void confirmDelete(final GioStore.Memorial memorial) {
        new AlertDialog.Builder(this)
                .setTitle("Xoá ngày giỗ")
                .setMessage("Xoá giỗ của " + memorial.name + "?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    GioStore.remove(GioSettingsActivity.this, memorial.id);
                    reload();
                    PrayerReminder.notifyToday(GioSettingsActivity.this);
                })
                .setNegativeButton("Giữ lại", null)
                .show();
    }

    private int yearAt(int position) {
        if (position < 0 || position >= years.size()) return currentYear;
        return years.get(position);
    }

    private ArrayAdapter<String> adapter(List<String> values) {
        return new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
    }

    private static void pick(Spinner spinner, int position) {
        if (position >= 0) spinner.setSelection(position);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private final class GioAdapter extends RecyclerView.Adapter<GioHolder> {

        @NonNull
        @Override
        public GioHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GioHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gio, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull GioHolder holder, int position) {
            GioStore.Memorial memorial = items.get(position);
            holder.name.setText(memorial.name);
            String term = Kinship.prayerTerm(family, familyRelations, familyRoot, memorial.personId);
            holder.relation.setText(term.isEmpty() ? "" : "Gọi theo gia phả: " + term);
            holder.relation.setVisibility(term.isEmpty() ? View.GONE : View.VISIBLE);
            holder.lunar.setText(memorial.lunarLabel());
            holder.solar.setText(memorial.solarLabel());
            holder.burial.setText("An táng: " + memorial.burialPlace);
            holder.burial.setVisibility(memorial.burialPlace.isEmpty() ? View.GONE : View.VISIBLE);
            holder.edit.setOnClickListener(v -> openEditor(memorial));
            holder.delete.setOnClickListener(v -> confirmDelete(memorial));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static final class GioHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView relation;
        final TextView lunar;
        final TextView solar;
        final TextView burial;
        final View edit;
        final View delete;

        GioHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.gio_name);
            relation = itemView.findViewById(R.id.gio_relation);
            lunar = itemView.findViewById(R.id.gio_lunar);
            solar = itemView.findViewById(R.id.gio_solar);
            burial = itemView.findViewById(R.id.gio_burial);
            edit = itemView.findViewById(R.id.gio_edit);
            delete = itemView.findViewById(R.id.gio_delete);
        }
    }
}
