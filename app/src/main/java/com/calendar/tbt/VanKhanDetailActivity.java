package com.calendar.tbt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Toàn văn một bài văn khấn, đã điền sẵn thông tin và tô sáng những chỗ tự điền. */
public class VanKhanDetailActivity extends AppCompatActivity {

    public static final String EXTRA_KEY = "com.calendar.tbt.extra.VK_KEY";
    public static final String EXTRA_TITLE = "com.calendar.tbt.extra.VK_TITLE";
    public static final String EXTRA_DAY = "com.calendar.tbt.extra.VK_DET_DAY";
    public static final String EXTRA_MONTH = "com.calendar.tbt.extra.VK_DET_MONTH";
    public static final String EXTRA_YEAR = "com.calendar.tbt.extra.VK_DET_YEAR";
    public static final String EXTRA_MEMORIAL_ID = "com.calendar.tbt.extra.VK_MEMORIAL";
    public static final String EXTRA_FIELD = "com.calendar.tbt.extra.VK_FIELD";

    private String key;
    private String focusField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_van_khan_detail);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        Intent intent = getIntent();
        key = intent.getStringExtra(EXTRA_KEY);
        String title = intent.getStringExtra(EXTRA_TITLE);
        focusField = intent.getStringExtra(EXTRA_FIELD);

        Toolbar toolbar = findViewById(R.id.toolbar);
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        params.topMargin = statusBarHeight;
        toolbar.setLayoutParams(params);
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.detail_title).setVisibility(android.view.View.GONE);
        buildFieldChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderBody();
    }

    private void renderBody() {
        PrayerCatalog catalog = PrayerVault.shared(this);
        PrayerCatalog.Entry entry = catalog.findEntry(key);
        LunarCalendar.LunarDate lunar = readLunarDate();
        GioStore.Memorial memorial = GioStore.find(this, getIntent().getStringExtra(EXTRA_MEMORIAL_ID));
        Map<String, String> values = PrayerFiller.values(this, lunar, memorial);

        TextView body = findViewById(R.id.detail_body);
        TextView legend = findViewById(R.id.detail_legend);
        String raw;
        try {
            raw = PrayerCatalog.readItem(this, key);
        } catch (IOException e) {
            body.setText("");
            legend.setText("");
            Toast.makeText(this, "Không đọc được bài văn khấn.", Toast.LENGTH_SHORT).show();
            return;
        }
        PrayerFiller.Rendered rendered = PrayerFiller.render(this, raw, values, focusField);
        body.setText(rendered.text);

        if (focusField != null) {
            legend.setText(String.format("Đang xem trường «%s» (tô đậm gạch chân). Chỗ tô vàng là còn trống, chưa có dữ liệu.",
                    catalog.fieldLabel(focusField)));
        } else if (rendered.missing > 0) {
            legend.setText(String.format("Chỗ tô xanh đã tự điền · còn %d chỗ tô vàng chưa có thông tin."
                    + " Chạm thẻ bên dưới toàn văn để sửa.", rendered.missing));
        } else if (entry != null && !entry.slots.isEmpty()) {
            legend.setText("Toàn bộ chỗ điền đã có sẵn thông tin.");
        } else {
            legend.setText("");
        }
    }

    private void buildFieldChips() {
        PrayerCatalog catalog = PrayerVault.shared(this);
        PrayerCatalog.Entry entry = catalog.findEntry(key);
        LinearLayout container = findViewById(R.id.detail_fields);
        container.removeAllViews();
        if (entry == null || entry.slots.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (String slot : entry.slots) {
            TextView chip = (TextView) inflater.inflate(R.layout.chip_prayer_field, container, false);
            chip.setText(catalog.fieldLabel(slot));
            chip.setOnClickListener(v -> {
                Intent intent = new Intent(this, PrayerFieldActivity.class);
                intent.putExtra(PrayerFieldActivity.EXTRA_FIELD, slot);
                startActivity(intent);
            });
            container.addView(chip);
        }
    }

    private LunarCalendar.LunarDate readLunarDate() {
        Intent intent = getIntent();
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = intent.getIntExtra(EXTRA_YEAR, -1);
        int month = intent.getIntExtra(EXTRA_MONTH, -1);
        int day = intent.getIntExtra(EXTRA_DAY, -1);
        if (year > 0 && month > 0 && day > 0) calendar.set(year, month - 1, day);
        try {
            return LunarCalendar.getLunarDate(calendar.get(java.util.Calendar.DAY_OF_MONTH),
                    calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.YEAR));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
