package app.aialertpay.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import java.util.*;

public final class VoiceLanguageActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.voice_language);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.done).setOnClickListener(v -> {
            if (getIntent().getBooleanExtra(SettingsActivity.EDIT, false)) finish();
            else startActivity(new android.content.Intent(this, ClosingMessageActivity.class));
        });
        findViewById(R.id.sample).setOnClickListener(v -> VoiceSample.play(this));
        findViewById(R.id.more_languages).setOnClickListener(v -> {
            String[] tags = VoiceSample.names.keySet().toArray(new String[0]);
            String[] labels = VoiceSample.names.values().toArray(new String[0]);
            new AlertDialog.Builder(this).setTitle("All 26 languages")
                    .setItems(labels, (dialog, index) -> select(tags[index]))
                    .setNegativeButton("Cancel", null).show();
        });
        render();
    }

    private void select(String tag) {
        getSharedPreferences("alerts", MODE_PRIVATE).edit().putString("voice_language", tag).apply();
        render();
    }

    private void render() {
        String saved = VoiceSample.choice(this);
        Set<String> tags = new LinkedHashSet<>(Arrays.asList("", "bho", "hi", "en", saved));
        RadioGroup group = findViewById(R.id.languages);
        group.removeAllViews();
        for (String tag : tags) {
            RadioButton row = new RadioButton(this);
            row.setText(tag.isEmpty() ? "Phone default (" + VoiceSample.names.get(VoiceSample.resolved("")) + ")" : VoiceSample.names.get(tag));
            row.setMinHeight(Math.round(52 * getResources().getDisplayMetrics().density));
            group.addView(row);
            row.setChecked(tag.equals(saved));
            row.setOnClickListener(v -> select(tag));
        }
    }
}
