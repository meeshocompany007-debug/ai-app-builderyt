package app.aialertpay.mobile;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;

public final class PaymentAppsActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.payment_apps);
        findViewById(R.id.back).setOnClickListener(view -> finish());
        addApp("PhonePe", "com.phonepe.app");
        addApp("Google Pay", "com.google.android.apps.nbu.paisa.user");
        addApp("Paytm", "net.one97.paytm");
        addApp("Amazon Pay", "in.amazon.mShop.android.shopping");
    }

    private void addApp(String label, String pkg) {
        SharedPreferences settings = getSharedPreferences("payment_sources", MODE_PRIVATE);
        Switch row = new Switch(this);
        row.setText(label);
        row.setTextSize(18);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setMinHeight(dp(64));
        row.setBackgroundResource(R.drawable.setup_card);
        row.setCompoundDrawablePadding(dp(12));
        try {
            Drawable icon = getPackageManager().getApplicationIcon(pkg);
            icon.setBounds(0, 0, dp(38), dp(38));
            row.setCompoundDrawablesRelative(icon, null, null, null);
        } catch (PackageManager.NameNotFoundException missing) {
            row.setText(label + " · Not installed");
            row.setEnabled(false);
        }
        row.setChecked(row.isEnabled() && settings.getBoolean(pkg, false));
        row.setOnCheckedChangeListener((button, checked) ->
                settings.edit().putBoolean(pkg, checked).apply());
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2);
        size.bottomMargin = dp(8);
        LinearLayout rows = findViewById(R.id.app_rows);
        rows.addView(row, size);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
