package app.aialertpay.mobile;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public final class HomeActivity extends Activity {
    private LocalDate chosen;
    private int period;
    private int sort;
    private Receipts receipts;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ContentObserver observer = new ContentObserver(handler) {
        @Override public void onChange(boolean selfChange) { refresh(); }
    };
    private final Runnable midnight = this::refresh;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.home);
        receipts = new Receipts(this);
        findViewById(R.id.open_team).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, TeamActivity.class));
        });
        findViewById(R.id.open_qr).setOnClickListener(v -> startActivity(new android.content.Intent(this, PaymentQrActivity.class)));
        if (state != null && state.containsKey("day")) chosen = LocalDate.ofEpochDay(state.getLong("day"));
        if (state != null) {
            period = state.getInt("period");
            sort = state.getInt("sort");
        }
        int[] filters = {R.id.today, R.id.week, R.id.month};
        for (int i = 0; i < filters.length; i++) {
            final int index = i;
            findViewById(filters[i]).setOnClickListener(v -> { period = index; chosen = null; refresh(); });
        }
        findViewById(R.id.sort).setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                .setTitle("Sort payments").setSingleChoiceItems(new String[]{"Newest first", "Oldest first", "Highest amount"}, sort,
                        (dialog, index) -> { sort = index; dialog.dismiss(); refresh(); }).show());
        findViewById(R.id.day).setOnClickListener(v -> {
            LocalDate day = chosen == null ? LocalDate.now() : chosen;
            new DatePickerDialog(this, (picker, y, m, d) -> {
                LocalDate selected = LocalDate.of(y, m + 1, d);
                chosen = selected;
                period = 3;
                refresh();
            }, day.getYear(), day.getMonthValue()-1, day.getDayOfMonth()).show();
        });
        SharedPreferences settings = getSharedPreferences("alerts", MODE_PRIVATE);
        Switch voice = findViewById(R.id.voice_on);
        voice.setChecked(settings.getBoolean("voice_on", true));
        voice.setOnCheckedChangeListener((button, on) -> settings.edit().putBoolean("voice_on", on).apply());
    }

    @Override protected void onStart() {
        super.onStart();
        getContentResolver().registerContentObserver(Receipts.CHANGES, false, observer);
        refresh();
    }
    @Override protected void onStop() {
        getContentResolver().unregisterContentObserver(observer);
        handler.removeCallbacks(midnight);
        super.onStop();
    }
    @Override protected void onDestroy() { receipts.close(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("period", period);
        out.putInt("sort", sort);
        if (chosen != null) out.putLong("day", chosen.toEpochDay());
    }
    private String money(long paise) {
        return "₹" + BigDecimal.valueOf(paise, 2).stripTrailingZeros().toPlainString();
    }
    private String platform(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "App unavailable";
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (android.content.pm.PackageManager.NameNotFoundException missing) { return pkg; }
    }
    private void refresh() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate day = chosen == null ? today : chosen;
        LocalDate until = day.plusDays(1);
        if (period == 1) {
            until = today.minusDays(today.getDayOfWeek().getValue() - 1);
            day = until.minusWeeks(1);
        } else if (period == 2) {
            until = today.withDayOfMonth(1);
            day = until.minusMonths(1);
        }
        int[] filters = {R.id.today, R.id.week, R.id.month, R.id.day};
        for (int i = 0; i < filters.length; i++) {
            Button filter = findViewById(filters[i]);
            filter.setTextColor(i == period ? 0xffffffff : 0xff70001c);
            filter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(i == period ? 0xff70001c : 0xfff6e6de));
        }
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String range = day.format(date);
        if (!until.equals(day.plusDays(1))) range += " – " + until.minusDays(1).format(date);
        long start = day.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = until.atStartOfDay(zone).toInstant().toEpochMilli();
        ArrayList<android.text.SpannableString> rows = new ArrayList<>();
        long total = 0;
        String owner = Receipts.owner();
        String order = sort == 1 ? "time ASC" : sort == 2 ? "paise DESC,time DESC" : "time DESC";
        if (owner != null) {
            try (Cursor cursor = receipts.getReadableDatabase().rawQuery(
                    "SELECT payer,paise,time,source,recipient,event FROM receipt WHERE owner=? AND time>=? AND time<? ORDER BY " + order + ",rowid DESC",
                    new String[]{owner, Long.toString(start), Long.toString(end)})) {
                DateTimeFormatter time = DateTimeFormatter.ofPattern("dd MMM · h:mm a", Locale.getDefault()).withZone(zone);
                while (cursor.moveToNext()) {
                    long amount = cursor.getLong(1);
                    total += amount;
                    String pkg = cursor.getString(3);
                    if (pkg == null) {
                        String[] key = cursor.getString(5).split("\\\\|", 3);
                        if (key.length == 3) pkg = key[1];
                    }
                    String app = platform(pkg);
                    String recipient = cursor.isNull(4) ? "Recipient unavailable" : cursor.getString(4);
                    String amountText = "+" + money(amount);
                    String heading = cursor.getString(0) + "  →  " + recipient;
                    android.text.SpannableString row = new android.text.SpannableString(heading + "\n" + app + "  ·  " + time.format(Instant.ofEpochMilli(cursor.getLong(2))) + "\n" + amountText);
                    row.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, heading.length(), 0);
                    row.setSpan(new android.text.style.ForegroundColorSpan(0xff137053), row.length()-amountText.length(), row.length(), 0);
                    rows.add(row);
                }
            }
        }
        ((TextView)findViewById(R.id.collection)).setText("Total collection   " + money(total) + "\n" + range + "  ·  " + rows.size() + " payments");
        ((TextView)findViewById(R.id.total)).setText("Total received   " + money(total));
        if (rows.isEmpty()) rows.add(new android.text.SpannableString("No incoming payments recorded in this period."));
        ((ListView)findViewById(R.id.payments)).setAdapter(new ArrayAdapter<>(this, R.layout.payment_row, rows));
        handler.removeCallbacks(midnight);
        handler.postDelayed(midnight, Math.max(1000, today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()-System.currentTimeMillis()));
    }
}
