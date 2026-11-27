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
        if (state != null && state.containsKey("day")) chosen = LocalDate.ofEpochDay(state.getLong("day"));
        findViewById(R.id.day).setOnClickListener(v -> {
            LocalDate day = chosen == null ? LocalDate.now() : chosen;
            new DatePickerDialog(this, (picker, y, m, d) -> {
                LocalDate selected = LocalDate.of(y, m + 1, d);
                chosen = selected.equals(LocalDate.now()) ? null : selected;
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
        if (chosen != null) out.putLong("day", chosen.toEpochDay());
    }
    private String money(long paise) {
        return "₹" + BigDecimal.valueOf(paise, 2).stripTrailingZeros().toPlainString();
    }
    private void refresh() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate day = chosen == null ? today : chosen;
        boolean current = day.equals(today);
        ((Button)findViewById(R.id.day)).setText((current ? "Today · " : "") + day.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        long start = day.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        ArrayList<String> rows = new ArrayList<>();
        long total = 0;
        String owner = Receipts.owner();
        if (owner != null) {
            try (Cursor cursor = receipts.getReadableDatabase().rawQuery(
                    "SELECT payer,paise,time FROM receipt WHERE owner=? AND time>=? AND time<? ORDER BY time DESC,rowid DESC",
                    new String[]{owner, Long.toString(start), Long.toString(end)})) {
                DateTimeFormatter time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(zone);
                while (cursor.moveToNext()) {
                    long amount = cursor.getLong(1);
                    total += amount;
                    rows.add(cursor.getString(0) + "   " + money(amount) + "   " + time.format(Instant.ofEpochMilli(cursor.getLong(2))));
                }
            }
        }
        ((TextView)findViewById(R.id.collection)).setText((current ? "Aaj ka collection" : "Day collection") + "\n" + money(total) + "\n" + rows.size() + " payments");
        ((TextView)findViewById(R.id.total)).setText("Total   " + money(total));
        if (rows.isEmpty()) rows.add("No payment alerts recorded for this date.");
        ((ListView)findViewById(R.id.payments)).setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
        handler.removeCallbacks(midnight);
        handler.postDelayed(midnight, Math.max(1000, today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()-System.currentTimeMillis()));
    }
}
