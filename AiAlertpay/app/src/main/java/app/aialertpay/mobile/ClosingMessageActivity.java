package app.aialertpay.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.RadioGroup;

public final class ClosingMessageActivity extends Activity {
    private final int[] ids = {R.id.amount_only, R.id.thanks, R.id.jai_bhim, R.id.radhe, R.id.custom};
    private final String[] keys = {"none", "thanks", "jai_bhim", "radhe", "custom"};
    private RadioGroup choices;
    private EditText custom;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.closing_message);
        choices = findViewById(R.id.closing_options);
        custom = findViewById(R.id.custom_text);
        SharedPreferences prefs = getSharedPreferences("alerts", MODE_PRIVATE);
        String saved = prefs.getString("closing_mode", "none");
        choices.check(ids[0]);
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(saved)) choices.check(ids[i]);
        custom.setText(prefs.getString("closing_text", ""));
        custom.setEnabled(choices.getCheckedRadioButtonId() == R.id.custom);
        choices.setOnCheckedChangeListener((group, id) -> custom.setEnabled(id == R.id.custom));
        findViewById(R.id.back).setOnClickListener(view -> finish());
        findViewById(R.id.sample).setOnClickListener(view -> applyChoice(false));
        findViewById(R.id.done).setOnClickListener(view -> applyChoice(true));
    }

    private void applyChoice(boolean save) {
        String mode = "none";
        for (int i = 0; i < ids.length; i++)
            if (choices.getCheckedRadioButtonId() == ids[i]) mode = keys[i];
        String text = custom.getText().toString().trim();
        if ("custom".equals(mode) && text.isEmpty()) {
            custom.setError("Enter your closing message");
            custom.requestFocus();
            return;
        }
        if (save) {
            getSharedPreferences("alerts", MODE_PRIVATE).edit()
                    .putString("closing_mode", mode).putString("closing_text", text).apply();
            if (!getIntent().getBooleanExtra(SettingsActivity.EDIT, false)) startActivity(new android.content.Intent(this, HomeActivity.class));
            finish();
        } else VoiceSample.announce(this, 2000L, mode, text);
    }
}
