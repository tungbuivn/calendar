package com.calendar.tbt;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Máy đọc văn khấn bằng TextToSpeech của thiết bị.
 * Toàn văn được SpeechText chặt thành từng hơi, mỗi hơi một lần speak xếp hàng nối tiếp;
 * đọc tới hơi nào báo tới đó để màn hình tô sáng và cuộn theo.
 *
 * <p>TechToSpeech gọi lại ở luồng nền nên mọi thông báo đều được đưa về luồng chính,
 * màn hình khỏi phải tự lo chuyện này.
 */
public final class SpeechReader implements TextToSpeech.OnInitListener {

    public interface Listener {
        /** Máy đọc sẵn sàng hay chưa; message giải thích khi chưa dùng được. */
        void onSpeechReady(boolean ready, String message);

        /** Hơi thứ {@code index} vừa bắt đầu đọc, -1 khi hàng đã trống. */
        void onSpeechPiece(int index);

        void onSpeechFinished();

        void onSpeechError(String message);
    }

    public enum State { IDLE, PLAYING, PAUSED }

    private static final Locale VIETNAMESE = new Locale("vi", "VN");
    private static final String SAMPLE = "Nam mô A Di Đà Phật, hôm nay là ngày rằm tháng Giêng.";

    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech engine;
    private List<SpeechText.Piece> queue = Collections.emptyList();
    private int index = -1;
    /** Mã lô đọc hiện tại, để lời gọi của lô cũ không còn tác dụng. */
    private String session = "";
    private State state = State.IDLE;
    private boolean ready;
    private String problem = "";

    public SpeechReader(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        engine = new TextToSpeech(this.context, this);
    }

    @Override
    public void onInit(int status) {
        if (engine == null) return;
        if (status != TextToSpeech.SUCCESS) {
            ready = false;
            problem = "Thiết bị chưa có bộ máy đọc văn bản.";
        } else {
            engine.setOnUtteranceProgressListener(new Progress());
            applySettings();
        }
        final boolean flags = ready;
        final String message = problem;
        main.post(new Runnable() {
            @Override
            public void run() {
                listener.onSpeechReady(flags, message);
            }
        });
    }

    /** Áp tốc độ, cao độ và giọng đã lưu; gọi lại khi người dùng về màn hình. */
    public void applySettings() {
        if (engine == null) return;
        engine.setSpeechRate(SpeechSettings.rate(context));
        engine.setPitch(SpeechSettings.pitch(context));
        Voice chosen = findVoice(SpeechSettings.voice(context));
        if (chosen != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            engine.setVoice(chosen);
            ready = true;
            problem = "";
            return;
        }
        Locale locale = localeOf(SpeechSettings.voiceLocale(context), VIETNAMESE);
        int available = engine.isLanguageAvailable(locale);
        ready = available == TextToSpeech.LANG_AVAILABLE
                || available == TextToSpeech.LANG_COUNTRY_AVAILABLE
                || available == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
        problem = ready ? "" : "Máy đọc chưa có giọng tiếng Việt. Hãy cài thêm giọng đọc trong phần Cài đặt hệ thống.";
        if (ready) engine.setLanguage(locale);
    }

    public State state() {
        return state;
    }

    public boolean isReady() {
        return ready;
    }

    public String problem() {
        return problem;
    }

    public int currentIndex() {
        return index;
    }

    /** Đọc từ đầu bài đang hiển thị. */
    public void play(List<SpeechText.Piece> pieces) {
        if (engine == null || pieces == null || pieces.isEmpty()) return;
        session = UUID.randomUUID().toString();
        queue = pieces;
        index = 0;
        speakCurrent(TextToSpeech.QUEUE_FLUSH);
    }

    /** Đọc thử một câu ngắn cho màn cài đặt. */
    public void preview() {
        if (engine == null) return;
        session = UUID.randomUUID().toString();
        queue = Collections.emptyList();
        index = -1;
        state = State.PLAYING;
        String id = utteranceId(0);
        engine.speak(SAMPLE, TextToSpeech.QUEUE_FLUSH, bundleOf(id), id);
    }

    /** Dừng hơi đang đọc; resume sẽ đọc lại hơi đó từ đầu. */
    public void pause() {
        if (engine == null || state != State.PLAYING) return;
        engine.stop();
        state = State.PAUSED;
    }

    public void resume() {
        if (engine == null || state != State.PAUSED) return;
        if (index < 0 || index >= queue.size()) {
            state = State.IDLE;
            return;
        }
        speakCurrent(TextToSpeech.QUEUE_FLUSH);
    }

    /** Dừng hẳn, quay về trạng thái chờ. */
    public void stop() {
        session = UUID.randomUUID().toString();
        queue = Collections.emptyList();
        index = -1;
        state = State.IDLE;
        if (engine != null) engine.stop();
    }

    public void shutdown() {
        if (engine != null) {
            engine.stop();
            engine.shutdown();
            engine = null;
        }
        state = State.IDLE;
        queue = Collections.emptyList();
        index = -1;
    }

    /** Intent mở màn cài đặt giọng đọc của hệ thống để cài thêm ngôn ngữ. */
    public Intent installIntent() {
        return new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
    }

    /** Giọng đọc máy cung cấp, tiếng Việt xếp trước cho dễ chọn. */
    public List<Voice> voices() {
        if (engine == null) return Collections.emptyList();
        Set<Voice> available = engine.getVoices();
        List<Voice> result = new ArrayList<>(available == null
                ? Collections.<Voice>emptyList() : available);
        Collections.sort(result, new Comparator<Voice>() {
            @Override
            public int compare(Voice left, Voice right) {
                int byLanguage = Boolean.compare(isVietnamese(right), isVietnamese(left));
                if (byLanguage != 0) return byLanguage;
                return nameOf(left).compareToIgnoreCase(nameOf(right));
            }
        });
        return result;
    }

    /** Nhãn hiển thị của một giọng đọc. */
    public static String voiceLabel(Voice voice) {
        if (voice == null) return "";
        Locale locale = voice.getLocale();
        StringBuilder text = new StringBuilder(nameOf(voice));
        if (locale != null) {
            text.append(" (").append(locale.getDisplayLanguage(locale));
            if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
                text.append(" ").append(locale.getCountry());
            }
            text.append(")");
        }
        return text.toString();
    }

    private Voice findVoice(String name) {
        if (name == null || name.isEmpty() || engine == null) return null;
        for (Voice voice : voices()) {
            if (voice != null && name.equals(voice.getName())) return voice;
        }
        return null;
    }

    private static String nameOf(Voice voice) {
        return voice == null || voice.getName() == null ? "" : voice.getName();
    }

    private static boolean isVietnamese(Voice voice) {
        Locale locale = voice == null ? null : voice.getLocale();
        return locale != null && "vi".equalsIgnoreCase(locale.getLanguage());
    }

    private static Locale localeOf(String tag, Locale fallback) {
        if (tag == null || tag.isEmpty()) return fallback;
        String[] parts = tag.split("_");
        if (parts.length == 1) return new Locale(parts[0]);
        return new Locale(parts[0], parts[1]);
    }

    private void speakCurrent(int queueMode) {
        if (engine == null || index < 0 || index >= queue.size()) {
            state = State.IDLE;
            return;
        }
        String id = utteranceId(index);
        state = State.PLAYING;
        engine.speak(queue.get(index).spoken, queueMode, bundleOf(id), id);
    }

    private String utteranceId(int position) {
        return session + ":" + position;
    }

    private static Bundle bundleOf(String utteranceId) {
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        return params;
    }

    /**
     * Báo cáo từ máy đọc. Hơi thuộc lô cũ bị bỏ vì người dùng đã bấm dừng hoặc đọc bài khác.
     */
    private final class Progress extends UtteranceProgressListener {

        @Override
        public void onStart(String utteranceId) {
            final int position = positionOf(utteranceId);
            if (position < 0) return;
            state = State.PLAYING;
            main.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSpeechPiece(position);
                }
            });
        }

        @Override
        public void onDone(String utteranceId) {
            int position = positionOf(utteranceId);
            if (position < 0) return;
            if (!queue.isEmpty()) {
                index = position + 1;
                if (index < queue.size()) {
                    speakCurrent(TextToSpeech.QUEUE_ADD);
                    return;
                }
            }
            index = -1;
            queue = Collections.emptyList();
            state = State.IDLE;
            main.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSpeechPiece(-1);
                    listener.onSpeechFinished();
                }
            });
        }

        @Override
        public void onError(String utteranceId) {
            if (positionOf(utteranceId) < 0) return;
            stop();
            main.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSpeechError("Máy đọc dừng giữa chừng. Hãy thử lại hoặc đổi giọng khác.");
                }
            });
        }

        private int positionOf(String utteranceId) {
            if (utteranceId == null || !utteranceId.startsWith(session + ":")) return -1;
            try {
                return Integer.parseInt(utteranceId.substring(session.length() + 1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }
}
