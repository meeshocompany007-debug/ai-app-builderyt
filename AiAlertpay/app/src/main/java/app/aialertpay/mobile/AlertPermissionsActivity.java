package app.aialertpay.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

public final class AlertPermissionsActivity extends Activity {
    private SeekBar volume;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.alert_permissions);
        findViewById(R.id.payment_apps).setOnClickListener(v ->
                startActivity(new Intent(this, PaymentAppsActivity.class)));
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.access).setOnClickListener(v -> open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        findViewById(R.id.post).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    && (!getPreferences(0).getBoolean("asked", false) || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
                getPreferences(0).edit().putBoolean("asked", true).apply();
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        });
        findViewById(R.id.battery).setOnClickListener(v -> open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        findViewById(R.id.autostart).setOnClickListener(v -> {
            toast("Look under Battery or background usage. Options depend on the phone.");
            open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        });
        volume = findViewById(R.id.volume);
        volume.setProgress(getSharedPreferences("alerts", MODE_PRIVATE).getInt("volume", 80));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
            public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (user) getSharedPreferences("alerts", MODE_PRIVATE).edit().putInt("volume", value).apply();
            }
        });
        findViewById(R.id.test).setOnClickListener(v -> VoiceSample.play(this));
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        refresh();
    }

    private void refresh() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        boolean granted = Build.VERSION.SDK_INT >= 27
                ? manager.isNotificationListenerAccessGranted(new ComponentName(this, PaymentNotificationListener.class))
                : hasLegacyGrant();
        ((Button) findViewById(R.id.access)).setText(granted ? "Allowed" : "Open settings");
        ((Button) findViewById(R.id.post)).setText(manager.areNotificationsEnabled() ? "Enabled" : "Allow");
    }

    private boolean hasLegacyGrant() {
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (listeners == null) return false;
        ComponentName service = new ComponentName(this, PaymentNotificationListener.class);
        for (String name : listeners.split(":")) {
            if (service.equals(ComponentName.unflattenFromString(name))) return true;
        }
        return false;
    }

    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) { toast("This setting is unavailable on this phone."); }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
