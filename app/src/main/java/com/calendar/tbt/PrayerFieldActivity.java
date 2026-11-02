package com.calendar.tbt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private EditText input;
    private List<PrayerCatalog.Entry> usages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_field);

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

        boolean auto = field != null && "date".equals(field.source);
        boolean memorial = field != null && "memorial".equals(field.source);
        input.setVisibility(auto ? View.GONE : View.VISIBLE);
        save.setVisibility(auto ? View.GONE : View.VISIBLE);
        gioButton.setVisibility(memorial ? View.VISIBLE : View.GONE);
        note.setText(buildNote(field, auto, memorial));
        if (!auto) input.setText(ProfileStore.get(this, fieldId));
        gioButton.setOnClickListener(v -> startActivity(new Intent(this, GioSettingsActivity.class)));
        save.setOnClickListener(v -> {
            ProfileStore.set(this, fieldId, input.getText().toString());
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

    private String buildNote(PrayerCatalog.Field field, boolean auto, boolean memorial) {
        if (field == null) return "Không tìm thấy trường này trong danh mục.";
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
