package app.aialertpay.mobile;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import java.util.Locale;

final class VoiceSample {
    static String choice(Context context) {
        return context.getSharedPreferences("alerts", Context.MODE_PRIVATE).getString("voice_language", "");
    }

    static Locale locale(String tag) {
        return tag.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(tag);
    }

    static void play(Context context, TextToSpeech engine) {
        Locale language = locale(choice(context));
        if (engine.setLanguage(language) < 0) {
            Toast.makeText(context, "Voice unavailable on this phone.", Toast.LENGTH_LONG).show();
            return;
        }
        String sample;
        switch (language.getLanguage()) {
            case "hi": sample = "बीस रुपये प्राप्त हुए।"; break;
            case "bho": sample = "बीस रुपइया मिलल बा।"; break;
            case "en": sample = "Twenty rupees received."; break;
            default: sample = language.getDisplayName(language) + ". 20 ₹";
        }
        Bundle options = new Bundle();
        int volume = context.getSharedPreferences("alerts", Context.MODE_PRIVATE).getInt("volume", 80);
        options.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume / 100f);
        if (engine.speak(sample, TextToSpeech.QUEUE_FLUSH, options, "voice_sample") == TextToSpeech.ERROR)
            Toast.makeText(context, "Unable to play this voice.", Toast.LENGTH_LONG).show();
    }
}
