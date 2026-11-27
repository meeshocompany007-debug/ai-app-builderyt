package app.aialertpay.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
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
        findViewById(R.id.permissions).setOnClickListener(v ->
                startActivity(new Intent(this, AlertPermissionsActivity.class)));
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
        Dialog sheet = new Dialog(this);
        sheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        panel.setPadding(pad, pad, pad, pad);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFFFFF9ED);
        background.setCornerRadii(new float[]{pad,pad,pad,pad,0,0,0,0});
        panel.setBackground(background);

        TextView title = new TextView(this);
        title.setText("Choose a UPI app");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Available on this phone");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        panel.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        for (Map.Entry<String, ResolveInfo> entry : apps.entrySet()) {
            String pkg = entry.getKey();
            ResolveInfo app = entry.getValue();
            Button row = new Button(this, null, android.R.attr.borderlessButtonStyle);
            row.setAllCaps(false);
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            row.setText(app.loadLabel(getPackageManager()));
            Drawable icon = app.loadIcon(getPackageManager());
            icon.setBounds(0, 0, dp(28), dp(28));
            row.setCompoundDrawablesRelative(icon, null, null, null);
            row.setCompoundDrawablePadding(dp(12));
            rows.addView(row, -1, dp(52));
            row.setOnClickListener(view -> {
                sheet.dismiss();
                select(pkg);
            });
        }
        int listLimit = getResources().getDisplayMetrics().heightPixels / 2;
        panel.addView(scroll, -1, Math.min(listLimit, apps.size() * dp(52)));
        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        cancel.setOnClickListener(view -> sheet.dismiss());
        panel.addView(cancel);
        sheet.setContentView(panel);
        Window window = sheet.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setWindowAnimations(android.R.style.Animation_InputMethod);
        sheet.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
