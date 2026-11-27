package app.aialertpay.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

public final class AlertPermissionsActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.alert_permissions);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.access).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        findViewById(R.id.post).setOnClickListener(v -> chooseNotificationAccess());
    }

    private void chooseNotificationAccess() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                (!getPreferences(MODE_PRIVATE).getBoolean("asked", false) ||
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("asked", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unavailable) {
            Toast.makeText(this, "Open notification settings on your phone.", Toast.LENGTH_LONG).show();
        }
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
        ComponentName listener = new ComponentName(this, PaymentNotificationListener.class);
        boolean allowed = Build.VERSION.SDK_INT >= 27
                ? manager.isNotificationListenerAccessGranted(listener) : legacyAccess(listener);
        ((Button) findViewById(R.id.access)).setText(allowed ? "Allowed · Manage" : "Open settings");
        ((Button) findViewById(R.id.post)).setText(manager.areNotificationsEnabled() ? "Enabled · Manage" : "Allow");
    }

    private boolean legacyAccess(ComponentName listener) {
        String grants = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (grants == null) return false;
        for (String value : grants.split(":")) {
            if (listener.equals(ComponentName.unflattenFromString(value))) return true;
        }
        return false;
    }
}
