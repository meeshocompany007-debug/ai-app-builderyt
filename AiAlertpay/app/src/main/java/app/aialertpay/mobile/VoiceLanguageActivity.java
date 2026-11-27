package app.aialertpay.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class VoiceLanguageActivity extends Activity {
    private TextToSpeech engine;
    private boolean ready;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.voice_language);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.done).setOnClickListener(v -> finish());
        findViewById(R.id.sample).setOnClickListener(v -> VoiceSample.play(this, engine));
        findViewById(R.id.more_languages).setOnClickListener(v -> more());
        render();
        engine = new TextToSpeech(this, result -> getWindow().getDecorView().post(() -> {
            if (isDestroyed() || isFinishing()) return;
            ready = result == TextToSpeech.SUCCESS;
            render();
            if (!ready) Toast.makeText(this, "Voice engine unavailable.", Toast.LENGTH_LONG).show();
        }));
    }

    private void select(String tag) {
        getSharedPreferences("alerts", MODE_PRIVATE).edit().putString("voice_language", tag).apply();
        render();
    }

    private void render() {
        String saved = VoiceSample.choice(this);
        List<String> tags = new ArrayList<>(Arrays.asList("", "hi-IN", "bho-IN", "en-IN"));
        if (!tags.contains(saved)) tags.add(saved);
        RadioGroup group = findViewById(R.id.languages);
        group.removeAllViews();
        for (String tag : tags) {
            Locale language = VoiceSample.locale(tag);
            String name = language.getDisplayLanguage();
            if (tag.isEmpty()) name = "Phone default (" + name + ")";
            else if (tag.equals("bho-IN")) name = "Bhojpuri";
            else if (tag.equals(saved) && tags.indexOf(tag) >= 4) name = language.getDisplayName();
            boolean available = ready && engine.isLanguageAvailable(language) >= 0;
            RadioButton row = new RadioButton(this);
            row.setText(ready && !available ? name + " · Voice unavailable on this phone" : name);
            row.setMinHeight(Math.round(52 * getResources().getDisplayMetrics().density));
            row.setEnabled(tag.isEmpty() || available);
            group.addView(row);
            row.setChecked(tag.equals(saved));
            row.setOnClickListener(v -> select(tag));
        }
        findViewById(R.id.sample).setEnabled(ready && engine.isLanguageAvailable(VoiceSample.locale(saved)) >= 0);
        findViewById(R.id.more_languages).setEnabled(ready);
    }

    private void more() {
        Set<Locale> available = engine.getAvailableLanguages();
        if (available == null || available.isEmpty()) {
            Toast.makeText(this, "No voice languages available.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Locale> ordered = new ArrayList<>(available);
        ordered.sort(Comparator.comparing((Locale item) -> item.getDisplayName(), String.CASE_INSENSITIVE_ORDER));
        String[] names = new String[ordered.size()];
        for (int i = 0; i < names.length; i++) names[i] = ordered.get(i).getDisplayName();
        new AlertDialog.Builder(this).setTitle("More languages").setItems(names,
                (dialog, index) -> select(ordered.get(index).toLanguageTag()))
                .setNegativeButton("Cancel", null).show();
    }

    @Override protected void onDestroy() {
        if (engine != null) { engine.stop(); engine.shutdown(); }
        super.onDestroy();
    }
}
