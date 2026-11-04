package com.calendar.tbt;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Toàn văn một bài văn khấn: đã điền sẵn thông tin, tô sáng chỗ tự điền,
 * chạm vào một chỗ điền là mở được hộp sửa trường đó, và có nút đọc thành tiếng
 * cả bài theo cấu hình ở màn «Giọng đọc văn khấn».
 */
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
    private TextView body;
    private NestedScrollView scroll;
    private List<PrayerFiller.Mark> marks = Collections.emptyList();

    private SpeechReader speech;
    private List<SpeechText.Piece> pieces = Collections.emptyList();
    private final List<Object> spokenSpans = new ArrayList<>();
    private FloatingActionButton speakFab;
    private FloatingActionButton stopFab;
    private boolean speechReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_van_khan_detail);
        AppChrome.setup(this, R.id.van_khan);

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
        toolbar.inflateMenu(R.menu.van_khan_detail_menu);
        toolbar.setOnMenuItemClickListener(this::onToolbarItem);
        speakFab = findViewById(R.id.fab_speak);
        stopFab = findViewById(R.id.fab_speak_stop);
        if (speakFab != null) speakFab.setOnClickListener(v -> toggleSpeech());
        if (stopFab != null) stopFab.setOnClickListener(v -> {
            speech.stop();
            clearHighlight();
            updateSpeakIcon();
        });

        findViewById(R.id.detail_title).setVisibility(View.GONE);
        body = findViewById(R.id.detail_body);
        scroll = findViewById(R.id.detail_scroll);
        final GestureDetector taps = new GestureDetector(this, new BodyTap());
        // Trả false để TextView vẫn bôi đen được chữ; detector chỉ lấy riêng cái chạm đơn.
        body.setOnTouchListener((v, event) -> {
            taps.onTouchEvent(event);
            return false;
        });
        buildFieldChips();

        speech = new SpeechReader(this, new Speech());
        updateSpeakIcon();
    }

    private boolean onToolbarItem(MenuItem item) {
        if (item.getItemId() == R.id.action_speak_settings) {
            startActivity(new Intent(this, TtsSettingsActivity.class));
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderBody();
        speech.applySettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Rời màn hình là thôi đọc, khỏi để máy lải nhải một mình khi người dùng đã đi.
        speech.stop();
        clearHighlight();
        updateSpeakIcon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speech.shutdown();
    }

    private void renderBody() {
        PrayerCatalog catalog = PrayerVault.shared(this);
        PrayerCatalog.Entry entry = catalog.findEntry(key);
        LunarCalendar.LunarDate lunar = readLunarDate();
        GioStore.Memorial memorial = GioStore.find(this, getIntent().getStringExtra(EXTRA_MEMORIAL_ID));
        Map<String, String> values = PrayerFiller.values(this, lunar, memorial);

        TextView legend = findViewById(R.id.detail_legend);
        String raw;
        try {
            raw = PrayerCatalog.readItem(this, key);
        } catch (IOException e) {
            body.setText("");
            legend.setText("");
            marks = Collections.emptyList();
            pieces = Collections.emptyList();
            speech.stop();
            clearHighlight();
            updateSpeakIcon();
            Toast.makeText(this, "Không đọc được bài văn khấn.", Toast.LENGTH_SHORT).show();
            return;
        }
        PrayerFiller.Rendered rendered = PrayerFiller.render(this, raw, values, focusField);
        body.setText(rendered.text, TextView.BufferType.SPANNABLE);
        marks = rendered.marks;
        pieces = SpeechText.split(rendered.text.toString());
        speech.stop();
        clearHighlight();
        updateSpeakIcon();

        if (focusField != null) {
            legend.setText(String.format("Đang xem trường «%s» (tô đậm gạch chân). Chỗ tô vàng là còn trống, chưa có dữ liệu.",
                    catalog.fieldLabel(focusField)));
        } else if (rendered.missing > 0) {
            legend.setText(String.format("Chỗ tô xanh đã tự điền · còn %d chỗ tô vàng chưa có thông tin."
                    + " Chạm thẳng vào chỗ chấm trong toàn văn để sửa trường đó.", rendered.missing));
        } else if (entry != null && !entry.slots.isEmpty()) {
            legend.setText("Toàn bộ chỗ điền đã có sẵn thông tin — chạm vào một chỗ đã tô để sửa.");
        } else {
            legend.setText("");
        }
    }

    /** Chạm vào một chỗ điền trong toàn văn thì mở luôn hộp cấu hình trường đó. */
    private final class BodyTap extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            openMarkedField(event.getX(), event.getY());
            return false;
        }
    }

    private void openMarkedField(float touchX, float touchY) {
        Layout layout = body.getLayout();
        if (layout == null || marks.isEmpty()) return;
        float x = touchX - body.getTotalPaddingLeft() + body.getScrollX();
        float y = touchY - body.getTotalPaddingTop() + body.getScrollY();
        int line = layout.getLineForVertical((int) y);
        // Vùng trắng cuối dòng không phải là chỗ điền.
        if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) return;
        PrayerFiller.Mark mark = markAt(layout.getOffsetForHorizontal(line, x));
        if (mark == null) return;
        startActivity(new Intent(this, PrayerFieldActivity.class)
                .putExtra(PrayerFieldActivity.EXTRA_FIELD, mark.field));
    }

    /** offset có thể rơi ngay sau ký tự bị chạm nên thử cả ký tự trước nó. */
    private PrayerFiller.Mark markAt(int offset) {
        PrayerFiller.Mark hit = findMark(offset);
        return hit != null ? hit : findMark(offset - 1);
    }

    private PrayerFiller.Mark findMark(int offset) {
        for (PrayerFiller.Mark mark : marks) {
            if (offset >= mark.start && offset < mark.end) return mark;
        }
        return null;
    }

    // ---- đọc thành tiếng --------------------------------------------------

    /** Nút đọc: đang đọc thì tạm dừng, đang tạm dừng thì đọc tiếp, còn lại đọc từ đầu. */
    private void toggleSpeech() {
        if (speech.state() == SpeechReader.State.PLAYING) {
            speech.pause();
            updateSpeakIcon();
            return;
        }
        if (speech.state() == SpeechReader.State.PAUSED) {
            speech.resume();
            updateSpeakIcon();
            return;
        }
        if (!speechReady) {
            Toast.makeText(this, speech.problem(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, TtsSettingsActivity.class));
            return;
        }
        if (pieces.isEmpty()) {
            Toast.makeText(this, "Chưa có bài khấn để đọc.", Toast.LENGTH_SHORT).show();
            return;
        }
        speech.play(pieces);
        updateSpeakIcon();
    }

    private void updateSpeakIcon() {
        if (speakFab == null) return;
        SpeechReader.State state = speech.state();
        speakFab.setImageResource(state == SpeechReader.State.PLAYING
                ? R.drawable.ic_tts_pause : R.drawable.ic_tts_play);
        String description = state == SpeechReader.State.PLAYING ? "Tạm dừng đọc"
                : state == SpeechReader.State.PAUSED ? "Đọc tiếp" : "Đọc bài khấn";
        speakFab.setContentDescription(description);
    }

    /** Tô nền đúng đoạn máy đang đọc rồi cuộn cho đoạn đó hiện lên màn hình. */
    private void highlight(int index) {
        clearHighlight();
        if (index < 0 || index >= pieces.size() || !(body.getText() instanceof Spannable)) return;
        SpeechText.Piece piece = pieces.get(index);
        Spannable text = (Spannable) body.getText();
        BackgroundColorSpan span = new BackgroundColorSpan(
                ContextCompat.getColor(this, R.color.prayerSpeakingBackground));
        text.setSpan(span, piece.start, piece.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spokenSpans.add(span);
        body.invalidate();
        scrollToPiece(piece);
    }

    private void clearHighlight() {
        if (!spokenSpans.isEmpty() && body.getText() instanceof Spannable) {
            Spannable text = (Spannable) body.getText();
            for (Object span : spokenSpans) text.removeSpan(span);
            body.invalidate();
        }
        spokenSpans.clear();
    }

    private void scrollToPiece(SpeechText.Piece piece) {
        Layout layout = body.getLayout();
        if (layout == null || scroll == null) return;
        int line = layout.getLineForVertical(piece.start);
        int contentTop = body.getTop() + body.getTotalPaddingTop();
        int lineTop = contentTop + layout.getLineTop(line);
        int lineBottom = contentTop + layout.getLineBottom(line);
        // Dòng đang đọc còn nguyên trong vùng nhìn thấy thì khỏi cuộn.
        if (lineTop >= scroll.getScrollY() && lineBottom <= scroll.getScrollY() + scroll.getHeight()) return;
        int contentHeight = scroll.getChildAt(0).getHeight();
        int maxScroll = Math.max(0, contentHeight - scroll.getHeight());
        scroll.smoothScrollTo(0, Math.max(0, Math.min(lineTop - dp(40), maxScroll)));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Máy đọc báo về: đổi icon theo hơi đang đọc và tô sáng hơi đó. */
    private final class Speech implements SpeechReader.Listener {

        @Override
        public void onSpeechReady(boolean ready, String message) {
            speechReady = ready;
        }

        @Override
        public void onSpeechPiece(int index) {
            highlight(index);
            updateSpeakIcon();
        }

        @Override
        public void onSpeechFinished() {
            clearHighlight();
            updateSpeakIcon();
        }

        @Override
        public void onSpeechError(String message) {
            clearHighlight();
            updateSpeakIcon();
            Toast.makeText(VanKhanDetailActivity.this, message, Toast.LENGTH_LONG).show();
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
