package app.aialertpay.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PaymentAppsActivity extends Activity {
    private SharedPreferences sources;
    private final Map<String, ApplicationInfo> displayed = new LinkedHashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        sources = getSharedPreferences("payment_sources", MODE_PRIVATE);
        setContentView(R.layout.payment_apps);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.done).setOnClickListener(v -> {
            if (getIntent().getBooleanExtra(SettingsActivity.EDIT, false)) finish();
            else startActivity(new Intent(this, VoiceLanguageActivity.class));
        });
        findViewById(R.id.more_apps).setOnClickListener(v -> showMore());
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void collect(Intent intent, Map<String, ApplicationInfo> target) {
        for (ResolveInfo item : getPackageManager().queryIntentActivities(intent, 0)) {
            ApplicationInfo app = item.activityInfo.applicationInfo;
            if (app.enabled && !getPackageName().equals(app.packageName))
                target.put(app.packageName, app);
        }
    }

    private List<ApplicationInfo> sorted(Map<String, ApplicationInfo> apps) {
        List<ApplicationInfo> ordered = new ArrayList<>(apps.values());
        ordered.sort(Comparator.comparing((ApplicationInfo app) -> label(app), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(app -> app.packageName));
        return ordered;
    }

    private String label(ApplicationInfo app) {
        return app.loadLabel(getPackageManager()).toString();
    }

    private void refresh() {
        displayed.clear();
        collect(new Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay")), displayed);
        // Preserve manually added apps and selections saved by older versions.
        for (String pkg : sources.getAll().keySet()) {
            try {
                ApplicationInfo app = getPackageManager().getApplicationInfo(pkg, 0);
                if (app.enabled) displayed.put(pkg, app);
            } catch (PackageManager.NameNotFoundException removed) {
                // An uninstalled app cannot be an active notification source.
            }
        }
        LinearLayout rows = findViewById(R.id.app_rows);
        rows.removeAllViews();
        for (ApplicationInfo app : sorted(displayed)) addApp(label(app), app.packageName);
        if (displayed.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No UPI apps detected. Use More apps to add your payment app.");
            rows.addView(empty);
        }
    }

    private void showMore() {
        Map<String, ApplicationInfo> others = new LinkedHashMap<>();
        collect(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), others);
        others.keySet().removeAll(displayed.keySet());
        List<ApplicationInfo> apps = sorted(others);
        if (apps.isEmpty()) {
            Toast.makeText(this, "No additional apps found.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = label(apps.get(i));
        new AlertDialog.Builder(this).setTitle("Choose your payment app")
                .setItems(names, (dialog, index) -> {
                    sources.edit().putBoolean(apps.get(index).packageName, true).apply();
                    refresh();
                }).setNegativeButton("Cancel", null).show();
    }

    private void addApp(String label, String pkg) {
        SharedPreferences settings = sources;
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
