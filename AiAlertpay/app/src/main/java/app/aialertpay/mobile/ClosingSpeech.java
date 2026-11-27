package app.aialertpay.mobile;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class ClosingSpeech {
    // Called on the serialized payment audio worker, after amount playback finishes.
    static void speak(Context context, String tag, String text) throws IOException {
        CountDownLatch init = new CountDownLatch(1);
        AtomicBoolean ready = new AtomicBoolean();
        TextToSpeech tts = new TextToSpeech(context, status -> {
            ready.set(status == TextToSpeech.SUCCESS);
            init.countDown();
        });
        try {
            if (!init.await(8, TimeUnit.SECONDS) || !ready.get())
                throw new IOException("Speech engine unavailable");
            if (tts.setLanguage(Locale.forLanguageTag(tag)) < 0 &&
                    tts.setLanguage(Locale.getDefault()) < 0)
                throw new IOException("No custom message voice");
            CountDownLatch done = new CountDownLatch(1);
            AtomicBoolean success = new AtomicBoolean();
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                public void onStart(String id) {}
                public void onDone(String id) { success.set(true); done.countDown(); }
                public void onError(String id) { done.countDown(); }
            });
            Bundle options = new Bundle();
            options.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,
                    context.getSharedPreferences("alerts", 0).getInt("volume", 80) / 100f);
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, options, "closing") == TextToSpeech.ERROR ||
                    !done.await(45, TimeUnit.SECONDS) || !success.get())
                throw new IOException("Custom message playback failed");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException(error);
        } finally {
            tts.stop();
            tts.shutdown();
        }
    }
}
