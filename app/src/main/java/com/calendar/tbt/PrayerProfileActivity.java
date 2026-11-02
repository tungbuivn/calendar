package com.calendar.tbt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Danh sách trường người cúng điền, grouped theo mục đích, mỗi trường mở sang màn hình cấu hình. */
public class PrayerProfileActivity extends AppCompatActivity {

    private static final int ROW_GROUP = 0;
    private static final int ROW_FIELD = 1;

    private final List<Row> rows = new ArrayList<>();

    private static final class Row {
        final int type;
        final String title;
        final String subtitle;
        final String count;
        final String fieldId;

        Row(int type, String title, String subtitle, String count, String fieldId) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.count = count;
            this.fieldId = fieldId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_profile);

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

        RecyclerView list = findViewById(R.id.field_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new FieldAdapter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuildRows();
        ((RecyclerView) findViewById(R.id.field_list)).getAdapter().notifyDataSetChanged();
    }

    private void rebuildRows() {
        rows.clear();
        PrayerCatalog catalog = PrayerVault.shared(this);
        String lastGroup = null;
        for (PrayerCatalog.Field field : catalog.fields()) {
            if (!field.group.equals(lastGroup)) {
                lastGroup = field.group;
                rows.add(new Row(ROW_GROUP, catalog.groupTitle(field.group), "", "", null));
            }
            String value = ProfileStore.get(this, field.id);
            String subtitle;
            if ("date".equals(field.source)) {
                subtitle = "Tự động theo ngày đang xem";
            } else if (value.isEmpty()) {
                subtitle = field.hint;
            } else {
                subtitle = value;
            }
            int usage = catalog.entriesUsing(field.id).size();
            rows.add(new Row(ROW_FIELD, field.label, subtitle,
                    usage == 0 ? "dẫn nhập" : usage + " bài", field.id));
        }
    }

    private final class FieldAdapter extends RecyclerView.Adapter<Holder> {

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).type;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == ROW_GROUP ? R.layout.item_van_khan_group : R.layout.item_prayer_field;
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Row row = rows.get(position);
            holder.title.setText(row.title);
            if (row.type == ROW_FIELD) {
                holder.subtitle.setText(row.subtitle);
                holder.count.setText(row.count);
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(PrayerProfileActivity.this, PrayerFieldActivity.class);
                    intent.putExtra(PrayerFieldActivity.EXTRA_FIELD, row.fieldId);
                    startActivity(intent);
                });
            } else {
                holder.count.setText("");
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView count;

        Holder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.row_title);
            TextView value = itemView.findViewById(R.id.field_value);
            TextView badge = itemView.findViewById(R.id.field_count);
            if (badge == null) badge = itemView.findViewById(R.id.group_count);
            subtitle = value;
            count = badge;
        }
    }
}
