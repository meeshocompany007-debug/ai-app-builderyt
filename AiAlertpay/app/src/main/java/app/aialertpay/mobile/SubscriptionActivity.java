package app.aialertpay.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SubscriptionActivity extends Activity {
    private final Map<String, ResolveInfo> apps = new LinkedHashMap<>();
    private String selected;
    private LinearLayout choices;
    private TextView message;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.subscription);
        findViewById(R.id.setup_audio).setOnClickListener(v ->
                startActivity(new Intent(this, AlertSetupActivity.class)));
        findViewById(R.id.hero).setClipToOutline(true);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        choices = findViewById(R.id.choices);
        message = findViewById(R.id.message);
        if (state != null) selected = state.getString("selected");
        findViewById(R.id.trial).setOnClickListener(v -> {
            if (selected == null) showApps();
            else checkoutStatus();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        PackageManager manager = getPackageManager();
        Intent lookup = new Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"));
        apps.clear();
        for (ResolveInfo info : manager.queryIntentActivities(lookup, PackageManager.MATCH_DEFAULT_ONLY)) {
            apps.put(info.activityInfo.packageName, info);
        }
        if (!apps.containsKey(selected)) selected = null;
        drawChoices();
    }

    private void drawChoices() {
        choices.removeAllViews();
        addChoice("Google Pay", "com.google.android.apps.nbu.paisa.user");
        addChoice("PhonePe", "com.phonepe.app");
        addChoice("Paytm", "net.one97.paytm");
        addChoice("More", null);
    }

    private void addChoice(String label, String pkg) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setText(label);
        button.setMinWidth(0);
        button.setPadding(2, 6, 2, 6);
        button.setContentDescription(label);
        if (pkg != null && apps.containsKey(pkg)) {
            Drawable icon = apps.get(pkg).loadIcon(getPackageManager());
            int size = Math.round(24 * getResources().getDisplayMetrics().density);
            icon.setBounds(0, 0, size, size);
            button.setCompoundDrawables(null, icon, null, null);
            if (pkg.equals(selected)) button.setText("✓ " + label);
        }
        choices.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
        button.setOnClickListener(v -> {
            if (pkg == null) showApps();
            else if (apps.containsKey(pkg)) select(pkg);
            else message.setText(label + " is unavailable. Tap More to choose another app.");
        });
    }

    private void showApps() {
        if (apps.isEmpty()) {
            message.setText("No UPI app found on this phone.");
            return;
        }
        String[] packages = apps.keySet().toArray(new String[0]);
        String[] names = new String[packages.length];
        for (int i = 0; i < packages.length; i++)
            names[i] = apps.get(packages[i]).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle("Choose a UPI app")
                .setItems(names, (dialog, index) -> select(packages[index]))
                .setNegativeButton("Cancel", null).show();
    }

    private void select(String pkg) {
        selected = pkg;
        drawChoices();
        checkoutStatus();
    }

    private void checkoutStatus() {
        String name = apps.get(selected).loadLabel(getPackageManager()).toString();
        message.setText(name + " selected. Merchant AutoPay setup is pending. No payment requested.");
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("selected", selected);
    }
}
