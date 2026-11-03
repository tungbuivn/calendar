package com.calendar.tbt;

import android.os.Bundle;
import android.speech.tts.Voice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chỉnh cách máy đọc đọc văn khấn: tốc độ, cao độ, giọng nào.
 * Thanh trượt lưu ngay khi buông tay nên quay lại bài khấn là đọc theo cấu hình mới.
 */
public class TtsSettingsActivity extends AppCompatActivity {

    private static final Locale VIET = new Locale("vi", "VN");

    private TextView rateLabel;
    private TextView pitchLabel;
    private TextView voiceState;
    private SeekBar rateSeek;
    private SeekBar pitchSeek;
    private Spinner voiceSpinner;
    private final List<Voice> voices = new ArrayList<>();
    private SpeechReader reader;
    /** Đang đặt lại thanh trượt theo dữ liệu đã lưu thì đừng ghi ngược xuống đĩa. */
    private boolean binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts_settings);

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

        rateLabel = findViewById(R.id.speech_rate_label);
        pitchLabel = findViewById(R.id.speech_pitch_label);
        voiceState = findViewById(R.id.speech_voice_state);
        rateSeek = findViewById(R.id.speech_rate_seek);
        pitchSeek = findViewById(R.id.speech_pitch_seek);
        voiceSpinner = findViewById(R.id.speech_voice_spinner);
        voiceSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));

        findViewById(R.id.speech_preview_button).setOnClickListener(v -> {
            if (reader.isReady()) reader.preview();
            else Toast.makeText(this, reader.problem(), Toast.LENGTH_LONG).show();
        });
        findViewById(R.id.speech_reset_button).setOnClickListener(v -> {
            SpeechSettings.reset(this);
            reader.applySettings();
            bindValues();
            Toast.makeText(this, "Đã trở về tốc độ và cao độ mặc định.", Toast.LENGTH_SHORT).show();
        });

        rateSeek.setOnSeekBarChangeListener(new Slider(true));
        pitchSeek.setOnSeekBarChangeListener(new Slider(false));
        voiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selected, int position, long id) {
                if (binding || position < 0 || position >= voices.size()) return;
                Voice voice = voices.get(position);
                SpeechSettings.setVoice(TtsSettingsActivity.this, voice.getName(),
                        voice.getLocale() == null ? "" : voice.getLocale().toString());
                reader.applySettings();
                showVoiceState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bindValues();
        reader = new SpeechReader(this, new SpeechReader.Listener() {
            @Override
            public void onSpeechReady(boolean ready, String message) {
                bindVoices();
                showVoiceState();
                if (!ready) voiceState.setText(message);
            }

            @Override
            public void onSpeechPiece(int index) {
            }

            @Override
            public void onSpeechFinished() {
            }

            @Override
            public void onSpeechError(String message) {
                Toast.makeText(TtsSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (reader != null) reader.shutdown();
        reader = null;
        super.onDestroy();
    }

    private void bindValues() {
        binding = true;
        float rate = SpeechSettings.rate(this);
        float pitch = SpeechSettings.pitch(this);
        rateSeek.setProgress(SpeechSettings.toProgress(rate, SpeechSettings.MIN_RATE, SpeechSettings.MAX_RATE));
        pitchSeek.setProgress(SpeechSettings.toProgress(pitch, SpeechSettings.MIN_PITCH, SpeechSettings.MAX_PITCH));
        rateLabel.setText(String.format(VIET, "Tốc độ đọc · %1$.1f lần", rate));
        pitchLabel.setText(String.format(VIET, "Cao độ · %1$.1f", pitch));
        binding = false;
    }

    /** Đổ danh sách giọng đọc máy cung cấp, chọn đúng giọng đã lưu. */
    private void bindVoices() {
        voices.clear();
        voices.addAll(reader.voices());
        List<String> labels = new ArrayList<>();
        if (voices.isEmpty()) {
            binding = true;
            voiceSpinner.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, labels));
            binding = false;
            return;
        }
        labels.add("Máy đọc tự chọn giọng theo ngôn ngữ");
        int chosen = 0;
        String saved = SpeechSettings.voice(this);
        for (int i = 0; i < voices.size(); i++) {
            labels.add(SpeechReader.voiceLabel(voices.get(i)));
            if (!saved.isEmpty() && saved.equals(voices.get(i).getName())) chosen = i + 1;
        }
        binding = true;
        voiceSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        voiceSpinner.setSelection(chosen);
        binding = false;
    }

    private void showVoiceState() {
        if (!reader.isReady()) {
            voiceState.setText(reader.problem());
            voiceState.setOnClickListener(v -> startActivity(reader.installIntent()));
            return;
        }
        voiceState.setOnClickListener(null);
        String saved = SpeechSettings.voice(this);
        voiceState.setText(saved.isEmpty()
                ? "Máy đọc đang tự chọn giọng, ưu tiên tiếng Việt."
                : "Đang dùng giọng: " + saved);
    }

    /** Thanh trượt: đổi nhãn ngay khi kéo, chỉ ghi xuống đĩa khi buông tay. */
    private final class Slider implements SeekBar.OnSeekBarChangeListener {
        private final boolean rate;

        Slider(boolean rate) {
            this.rate = rate;
        }

        @Override
        public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
            if (binding) return;
            float value = SpeechSettings.toScale(progress,
                    rate ? SpeechSettings.MIN_RATE : SpeechSettings.MIN_PITCH,
                    rate ? SpeechSettings.MAX_RATE : SpeechSettings.MAX_PITCH);
            if (rate) {
                rateLabel.setText(String.format(VIET, "Tốc độ đọc · %1$.1f lần", value));
            } else {
                pitchLabel.setText(String.format(VIET, "Cao độ · %1$.1f", value));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seek) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seek) {
            if (binding) return;
            float value = SpeechSettings.toScale(seek.getProgress(),
                    rate ? SpeechSettings.MIN_RATE : SpeechSettings.MIN_PITCH,
                    rate ? SpeechSettings.MAX_RATE : SpeechSettings.MAX_PITCH);
            if (rate) SpeechSettings.setRate(TtsSettingsActivity.this, value);
            else SpeechSettings.setPitch(TtsSettingsActivity.this, value);
            reader.applySettings();
        }
    }
}
