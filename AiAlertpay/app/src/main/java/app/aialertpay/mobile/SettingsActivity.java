package app.aialertpay.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;

public final class SettingsActivity extends Activity {
    static final String EDIT = "edit_from_settings";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.app_settings);
        SharedPreferences alerts = getSharedPreferences("alerts", MODE_PRIVATE);
        Switch voice = findViewById(R.id.voice_on);
        voice.setChecked(alerts.getBoolean("voice_on", true));
        voice.setOnCheckedChangeListener((button, checked) -> alerts.edit().putBoolean("voice_on", checked).apply());
        editor(R.id.settings_language, VoiceLanguageActivity.class);
        editor(R.id.settings_closing, ClosingMessageActivity.class);
        editor(R.id.settings_apps, PaymentAppsActivity.class);
        editor(R.id.settings_permissions, AlertPermissionsActivity.class);
        findViewById(R.id.settings_subscription).setOnClickListener(v ->
                startActivity(new Intent(this, PlanDetailsActivity.class)));
        editor(R.id.settings_address, PaymentQrActivity.class);
        findViewById(R.id.settings_test).setOnClickListener(v -> VoiceSample.announce(this, 2000L));
        findViewById(R.id.open_collection).setOnClickListener(v -> finish());
        tab(R.id.open_team, TeamActivity.class);
        tab(R.id.open_qr, PaymentQrActivity.class);
    }

    private void editor(int id, Class<? extends Activity> screen) {
        findViewById(id).setOnClickListener(v -> startActivity(new Intent(this, screen).putExtra(EDIT, true)));
    }

    private void tab(int id, Class<? extends Activity> screen) {
        findViewById(id).setOnClickListener(v -> {
            startActivity(new Intent(this, screen));
            finish();
        });
    }
}
