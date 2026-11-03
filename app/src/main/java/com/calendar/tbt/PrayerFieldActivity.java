package com.calendar.tbt;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Cấu hình một trường điền và xem những bài khấn nào dùng trường đó. */
public class PrayerFieldActivity extends AppCompatActivity {

    public static final String EXTRA_FIELD = "com.calendar.tbt.extra.PF_FIELD";

    private String fieldId;
    private MultiAutoCompleteTextView input;
    private List<PrayerCatalog.Entry> usages;
    private LinearLayout worshipperQuick;
    private View recentGroupSection;
    private View recentNameSection;
    private LinearLayout recentGroupChips;
    private LinearLayout recentNameChips;
    private TextView saveGroupButton;
    private boolean worshipperField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_field);
        AppChrome.setup(this, R.id.today);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        fieldId = getIntent().getStringExtra(EXTRA_FIELD);
        PrayerCatalog catalog = PrayerVault.shared(this);
        PrayerCatalog.Field field = catalog.field(fieldId);

        Toolbar toolbar = findViewById(R.id.toolbar);
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            params.topMargin = getResources().getDimensionPixelSize(resourceId);
            toolbar.setLayoutParams(params);
        }
        toolbar.setTitle(field == null ? "Trường điền" : field.label);
        toolbar.setNavigationOnClickListener(v -> finish());

        input = findViewById(R.id.field_input);
        Button save = findViewById(R.id.save_button);
        TextView note = findViewById(R.id.field_note);
        View gioButton = findViewById(R.id.open_gio_button);
        worshipperQuick = findViewById(R.id.worshipper_quick);
        recentGroupSection = findViewById(R.id.recent_group_section);
        recentNameSection = findViewById(R.id.recent_name_section);
        recentGroupChips = findViewById(R.id.recent_group_chips);
        recentNameChips = findViewById(R.id.recent_name_chips);
        saveGroupButton = findViewById(R.id.save_group_button);

        worshipperField = Worshippers.NAME_FIELD.equals(fieldId);
        boolean auto = field != null && "date".equals(field.source);
        boolean memorial = field != null && "memorial".equals(field.source);
        input.setVisibility(auto ? View.GONE : View.VISIBLE);
        save.setVisibility(auto ? View.GONE : View.VISIBLE);
        gioButton.setVisibility(memorial ? View.VISIBLE : View.GONE);
        note.setText(buildNote(field, auto, memorial));
        if (!auto) input.setText(ProfileStore.get(this, fieldId));
        if (worshipperField && !auto) {
            input.setHint("Gõ tên, cách nhau dấu phẩy");
            setupWorshipperQuick();
        }
        gioButton.setOnClickListener(v -> startActivity(new Intent(this, GioSettingsActivity.class)));
        save.setOnClickListener(v -> {
            String value = input.getText().toString();
            ProfileStore.set(this, fieldId, value);
            if (worshipperField) {
                WorshipperHistory.record(this, value);
                refreshWorshipperQuick();
            }
            Toast.makeText(this, "Đã lưu. Vào lại bài khấn để thấy chỗ điền đổi theo.", Toast.LENGTH_SHORT).show();
        });

        usages = field == null ? java.util.Collections.<PrayerCatalog.Entry>emptyList()
                : catalog.entriesUsing(fieldId);
        RecyclerView list = findViewById(R.id.prayer_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new UsageAdapter());
        TextView header = findViewById(R.id.usage_header);
        header.setText(usages.isEmpty() ? "Bài khấn dùng trường này"
                : String.format("%d bài khấn dùng trường này", usages.size()));
    }

    private void setupWorshipperQuick() {
        worshipperQuick.setVisibility(View.VISIBLE);
        input.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        input.setThreshold(1);
        saveGroupButton.setOnClickListener(v -> {
            String value = input.getText().toString();
            if (Worshippers.split(value).isEmpty()) {
                Toast.makeText(this, "Nhập ít nhất một tên trước.", Toast.LENGTH_SHORT).show();
                return;
            }
            ProfileStore.set(this, fieldId, value);
            WorshipperHistory.record(this, value);
            refreshWorshipperQuick();
            Toast.makeText(this, "Đã lưu nhóm để lần sau chọn nhanh.", Toast.LENGTH_SHORT).show();
        });
        refreshWorshipperQuick();
    }

    private void refreshWorshipperQuick() {
        if (!worshipperField) return;
        recentGroupChips.removeAllViews();
        recentNameChips.removeAllViews();

        List<String> groups = WorshipperHistory.groups(this);
        recentGroupSection.setVisibility(groups.isEmpty() ? View.GONE : View.VISIBLE);
        for (String group : groups) {
            recentGroupChips.addView(makeChip(group, true, v -> {
                input.setText(group);
                input.setSelection(input.getText().length());
            }));
        }

        List<String> names = WorshipperHistory.names(this);
        recentNameSection.setVisibility(names.isEmpty() ? View.GONE : View.VISIBLE);
        for (String name : names) {
            recentNameChips.addView(makeChip(name, false, v -> appendWorshipper(name)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        input.setAdapter(adapter);
    }

    private TextView makeChip(String text, boolean group, View.OnClickListener listener) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(13);
        chip.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        chip.setBackgroundResource(R.drawable.worshipper_chip_background);
        chip.setPadding(dp(12), dp(6), dp(12), dp(6));
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setClickable(true);
        chip.setFocusable(true);
        if (group) chip.setMaxWidth(dp(280));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(6));
        chip.setLayoutParams(params);
        chip.setOnClickListener(listener);
        return chip;
    }

    private void appendWorshipper(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return;
        for (String existing : Worshippers.split(input.getText().toString())) {
            if (existing.equalsIgnoreCase(trimmed)) return;
        }
        String current = input.getText().toString().trim();
        if (!current.isEmpty()) current = current + ", ";
        input.setText(current + trimmed);
        input.setSelection(input.getText().length());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private String buildNote(PrayerCatalog.Field field, boolean auto, boolean memorial) {
        if (field == null) return "Không tìm thấy trường này trong danh mục.";
        if (Worshippers.NAME_FIELD.equals(field.id)) {
            return field.hint + " Chọn nhóm/tên bên dưới hoặc gõ trực tiếp; từ 2 người bài khấn tự đổi sang «chúng con».";
        }
        if (auto) {
            return "Trường này tự điền theo ngày dương đang xem (" + field.hint + "), không cần cấu hình."
                    + " Chạm một bài bên dưới để xem nội dung trình bày như thế nào.";
        }
        if (memorial) {
            return "Khi mở bài từ khu «Văn khấn cho ngày» của một ngày giỗ đã cài, chỗ này tự lấy theo hồ sơ giỗ"
                    + " (" + field.hint + ")."
                    + " Bạn cũng có thể điền sẵn ở đây để dùng cho các bài khấn khác.";
        }
        return field.hint + ". Chạm một bài bên dưới để xem chỗ điền này xuất hiện ở đâu.";
    }

    /** Mở bài khấn ở chế độ xem thử: chỉ tô đậm đúng trường đang cấu hình. */
    private void preview(PrayerCatalog.Entry entry) {
        Intent intent = new Intent(this, VanKhanDetailActivity.class);
        intent.putExtra(VanKhanDetailActivity.EXTRA_KEY, entry.key);
        intent.putExtra(VanKhanDetailActivity.EXTRA_TITLE, entry.title);
        intent.putExtra(VanKhanDetailActivity.EXTRA_FIELD, fieldId);
        List<GioStore.Memorial> memorials = GioStore.all(this);
        if (!memorials.isEmpty()) {
            intent.putExtra(VanKhanDetailActivity.EXTRA_MEMORIAL_ID, memorials.get(0).id);
        }
        startActivity(intent);
    }

    private final class UsageAdapter extends RecyclerView.Adapter<UsageHolder> {

        @NonNull
        @Override
        public UsageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UsageHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_day_prayer, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UsageHolder holder, int position) {
            PrayerCatalog.Entry entry = usages.get(position);
            holder.title.setText(entry.title);
            holder.label.setText(entry.path);
            holder.itemView.setVisibility(usages.isEmpty() ? View.GONE : View.VISIBLE);
            holder.itemView.setOnClickListener(v -> preview(entry));
        }

        @Override
        public int getItemCount() {
            return usages.size();
        }
    }

    private static final class UsageHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView label;

        UsageHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.row_title);
            label = itemView.findViewById(R.id.day_prayer_label);
            itemView.findViewById(R.id.day_prayer_mark)
                    .setVisibility(android.view.View.INVISIBLE);
        }
    }
}
