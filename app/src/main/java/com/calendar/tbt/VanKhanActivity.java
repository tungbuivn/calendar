package com.calendar.tbt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.calendar.tbt.PrayerCatalog.Entry;
import com.calendar.tbt.PrayerCatalog.Group;
import com.calendar.tbt.PrayerCatalog.Part;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Màn hình Văn khấn: khu 1 gợi ý bài khấn theo ngày đang xem, khu 2 là mục lục toàn bộ bài.
 */
public class VanKhanActivity extends AppCompatActivity {

    public static final String EXTRA_DAY = "com.calendar.tbt.extra.VK_DAY";
    public static final String EXTRA_MONTH = "com.calendar.tbt.extra.VK_MONTH";
    public static final String EXTRA_YEAR = "com.calendar.tbt.extra.VK_YEAR";

    private static final int ROW_PART = 0;
    private static final int ROW_GROUP = 1;
    private static final int ROW_ENTRY = 2;

    private final List<Row> rows = new ArrayList<>();
    private final Set<Integer> expandedGroups = new HashSet<>();
    private final TocAdapter adapter = new TocAdapter();

    private List<Part> parts = new ArrayList<>();
    private int solarDay, solarMonth, solarYear;

    private static final class Row {
        final int type;
        final String title;
        final int groupId;
        final int entryCount;
        final int number;
        final Entry entry;

        Row(int type, String title, int groupId, int entryCount, int number, Entry entry) {
            this.type = type;
            this.title = title;
            this.groupId = groupId;
            this.entryCount = entryCount;
            this.number = number;
            this.entry = entry;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_van_khan);
        AppChrome.setup(this, R.id.van_khan);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        Toolbar toolbar = findViewById(R.id.toolbar);
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        params.topMargin = statusBarHeight;
        toolbar.setLayoutParams(params);
        toolbar.setNavigationOnClickListener(v -> finish());

        readSelectedDate();
        showSelectedDate();
        renderDayPrayers();

        findViewById(R.id.gio_settings_button).setOnClickListener(
                v -> startActivity(new Intent(this, GioSettingsActivity.class)));
        findViewById(R.id.profile_button).setOnClickListener(
                v -> startActivity(new Intent(this, PrayerProfileActivity.class)));

        RecyclerView tocList = findViewById(R.id.toc_list);
        tocList.setLayoutManager(new LinearLayoutManager(this));
        tocList.setAdapter(adapter);

        try {
            parts = PrayerVault.shared(this).parts();
        } catch (Exception e) {
            tocList.setVisibility(View.GONE);
            Toast.makeText(this, "Không đọc được danh mục văn khấn.", Toast.LENGTH_SHORT).show();
            return;
        }
        rebuildRows();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderDayPrayers();
    }

    private void readSelectedDate() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = getIntent().getIntExtra(EXTRA_YEAR, -1);
        int month = getIntent().getIntExtra(EXTRA_MONTH, -1);
        int day = getIntent().getIntExtra(EXTRA_DAY, -1);
        if (year > 0 && month > 0 && day > 0) calendar.set(year, month - 1, day);
        solarDay = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        solarMonth = calendar.get(java.util.Calendar.MONTH) + 1;
        solarYear = calendar.get(java.util.Calendar.YEAR);
    }

    private void showSelectedDate() {
        String solar = String.format("%d/%d/%d", solarDay, solarMonth, solarYear);
        TextView dateLabel = findViewById(R.id.day_prayer_date);
        try {
            LunarCalendar.LunarDate lunar = LunarCalendar.getLunarDate(solarDay, solarMonth, solarYear);
            dateLabel.setText(String.format("%s · Âm lịch %d/%d năm %s",
                    solar, lunar.day, lunar.month, lunar.getYearName()));
        } catch (IllegalArgumentException e) {
            dateLabel.setText(solar);
        }
    }

    /** Khu 1: những bài áp dụng cho ngày đang xem, gồm cả lễ giỗ đã cài. */
    private void renderDayPrayers() {
        LinearLayout container = findViewById(R.id.day_prayer_list);
        TextView empty = findViewById(R.id.day_prayer_empty);
        container.removeAllViews();

        List<PrayerDayMatcher.Suggestion> suggestions =
                PrayerDayMatcher.forDate(this, PrayerVault.shared(this), solarDay, solarMonth, solarYear);
        empty.setVisibility(suggestions.isEmpty() ? View.VISIBLE : View.GONE);
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        int green = androidx.core.content.ContextCompat.getColor(this, R.color.accentColor);
        int pink = androidx.core.content.ContextCompat.getColor(this, R.color.secondaryColor);
        for (PrayerDayMatcher.Suggestion suggestion : suggestions) {
            View row = inflater.inflate(R.layout.item_day_prayer, container, false);
            ((TextView) row.findViewById(R.id.row_title)).setText(suggestion.entry.title);
            ((TextView) row.findViewById(R.id.day_prayer_label)).setText(suggestion.label);
            row.findViewById(R.id.day_prayer_mark)
                    .setBackgroundColor(suggestion.isMemorial() ? pink : green);
            row.setOnClickListener(v -> openDetail(suggestion.entry, suggestion.memorial));
            container.addView(row);
        }
    }

    private void rebuildRows() {
        rows.clear();
        int groupId = 0;
        for (Part part : parts) {
            rows.add(new Row(ROW_PART, part.title, groupId, 0, 0, null));
            for (Group group : part.groups) {
                rows.add(new Row(ROW_GROUP, group.title, groupId, group.entries.size(), 0, null));
                if (expandedGroups.contains(groupId)) {
                    int number = 1;
                    for (Entry entry : group.entries) {
                        rows.add(new Row(ROW_ENTRY, entry.title, groupId, 0, number++, entry));
                    }
                }
                groupId++;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void toggleGroup(int groupId) {
        if (!expandedGroups.remove(groupId)) expandedGroups.add(groupId);
        rebuildRows();
    }

    private void openDetail(Entry entry) {
        openDetail(entry, null);
    }

    private void openDetail(Entry entry, GioStore.Memorial memorial) {
        Intent intent = new Intent(this, VanKhanDetailActivity.class);
        intent.putExtra(VanKhanDetailActivity.EXTRA_KEY, entry.key);
        intent.putExtra(VanKhanDetailActivity.EXTRA_TITLE, entry.title);
        intent.putExtra(VanKhanDetailActivity.EXTRA_DAY, solarDay);
        intent.putExtra(VanKhanDetailActivity.EXTRA_MONTH, solarMonth);
        intent.putExtra(VanKhanDetailActivity.EXTRA_YEAR, solarYear);
        if (memorial != null) {
            intent.putExtra(VanKhanDetailActivity.EXTRA_MEMORIAL_ID, memorial.id);
        }
        startActivity(intent);
    }

    private final class TocAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout = viewType == ROW_PART ? R.layout.item_van_khan_part
                    : viewType == ROW_GROUP ? R.layout.item_van_khan_group
                    : R.layout.item_van_khan_entry;
            return new Holder(inflater.inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            Holder h = (Holder) holder;
            h.title.setText(row.title);
            if (row.type == ROW_ENTRY) {
                h.number.setText(String.valueOf(row.number));
                h.itemView.setOnClickListener(v -> openDetail(row.entry));
            } else if (row.type == ROW_GROUP) {
                boolean open = expandedGroups.contains(row.groupId);
                h.count.setText(row.entryCount + (open ? " ▾" : " ▸"));
                h.itemView.setOnClickListener(v -> toggleGroup(row.groupId));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView number;
        final TextView count;

        Holder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.row_title);
            number = itemView.findViewById(R.id.entry_number);
            count = itemView.findViewById(R.id.group_count);
        }
    }
}
