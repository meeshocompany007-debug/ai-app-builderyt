package app.aialertpay.mobile;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

final class VoiceSample {
    static final Map<String, String> names;
    static {
        Map<String, String> languages = new LinkedHashMap<>();
        languages.put("hi", "Hindi");
        languages.put("bho", "Bhojpuri");
        languages.put("en", "English");
        languages.put("bn", "Bengali");
        languages.put("mr", "Marathi");
        languages.put("gu", "Gujarati");
        languages.put("ta", "Tamil");
        languages.put("te", "Telugu");
        languages.put("kn", "Kannada");
        languages.put("ml", "Malayalam");
        languages.put("pa", "Punjabi");
        languages.put("as", "Assamese");
        languages.put("or", "Odia");
        languages.put("ne", "Nepali");
        languages.put("kok", "Konkani");
        languages.put("ur", "Urdu");
        languages.put("mai", "Maithili");
        languages.put("mag", "Magahi");
        languages.put("awa", "Awadhi");
        languages.put("bra", "Braj");
        languages.put("mwr", "Marwari");
        languages.put("hne", "Chhattisgarhi");
        languages.put("bgc", "Haryanvi");
        languages.put("doi", "Dogri");
        languages.put("sa", "Sanskrit");
        languages.put("sd", "Sindhi");
        names = Collections.unmodifiableMap(languages);
    }

    static String choice(Context c) {
        String saved = c.getSharedPreferences("alerts", 0).getString("voice_language", "");
        String tag = Locale.forLanguageTag(saved).getLanguage();
        return names.containsKey(tag) ? tag : "";
    }

    static String resolved(String tag) {
        if (names.containsKey(tag)) return tag;
        String phone = Locale.getDefault().getLanguage();
        return names.containsKey(phone) ? phone : "hi";
    }

    static void play(Context context) {
        announce(context, 2000L);
    }

    // Input uses integer paise to keep every digit exact.
    static void announce(Context context, long amountPaise) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("alerts", 0);
        announce(context, amountPaise, prefs.getString("closing_mode", "none"), prefs.getString("closing_text", ""));
    }

    static void announce(Context context, long amountPaise, String mode, String custom) {
        if (amountPaise < 0) throw new IllegalArgumentException("Negative payment");
        String tag = resolved(choice(context));
        List<String> clips = new ArrayList<>();
        if (amountPaise == 2000) clips.add("sample");
        else {
            addDigits(clips, amountPaise / 100);
            clips.add("rupee");
            if (amountPaise % 100 != 0) {
                addDigits(clips, amountPaise % 100);
                clips.add("paise");
            }
            clips.add("received");
        }
        Context app = context.getApplicationContext();
        new Thread(() -> playClips(app, tag, clips, mode, custom), "payment-audio").start();
    }

    private static void addDigits(List<String> clips, long number) {
        for (char digit : Long.toString(number).toCharArray()) clips.add(String.valueOf(digit));
    }

    private static synchronized void playClips(Context app, String tag, List<String> clips, String mode, String custom) {
        AudioTrack track = null;
        try {
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            List<String> paths = new ArrayList<>();
            for (String clip : clips) paths.add(tag + "/" + clip);
            if (Arrays.asList("thanks", "jai_bhim", "radhe").contains(mode)) paths.add("endings/" + mode);
            for (String path : paths) {
                try (InputStream input = app.getAssets().open("voices/" + path + ".pcm")) {
                    int count;
                    while ((count = input.read(buffer)) != -1) pcm.write(buffer, 0, count);
                }
            }
            byte[] audio = pcm.toByteArray();
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(22050)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(audio.length).setTransferMode(AudioTrack.MODE_STATIC).build();
            if (track.write(audio, 0, audio.length) != audio.length) throw new IOException("Audio write failed");
            track.setVolume(app.getSharedPreferences("alerts", 0).getInt("volume", 80) / 100f);
            track.play();
            long deadline = android.os.SystemClock.elapsedRealtime() + audio.length * 1000L / 44100 + 2000;
            while (track.getPlaybackHeadPosition() < audio.length / 2 && android.os.SystemClock.elapsedRealtime() < deadline)
                android.os.SystemClock.sleep(30);
            if ("custom".equals(mode) && !custom.isEmpty())
                ClosingSpeech.speak(app, tag, custom);
        } catch (IOException | RuntimeException error) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(app, "Could not play voice.", Toast.LENGTH_LONG).show());
        } finally {
            if (track != null) track.release();
        }
    }
}
