package app.aialertpay.mobile;

import android.app.Notification;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentNotificationListener extends NotificationListenerService {
    private static final String MONEY = "(?:₹|INR\\s*|Rs\\.?\\s*)\\s*(\\d{1,9}(?:,\\d{2,3})*(?:\\.\\d{1,2})?)(?![\\d.,]\\d)";
    private static final Pattern RECEIVED = Pattern.compile("^(?:(?:you\\s+)?received\\s+" + MONEY
            + "|" + MONEY + "\\s+(?:received|credited))(?=\\s|[.!]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCLUDE = Pattern.compile("\\b(request(?:ed)?|pending|failed|reversed|refund(?:ed)?|cashback|reward|collect|offer|debited|sent|demo|sample|reminder|up to)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM = Pattern.compile("\\bfrom\\s+([\\p{L}\\p{M}][\\p{L}\\p{M} .'-]{0,64}?)(?=\\s+(?:via|on|at|to|UPI|ref)\\b|[.!\\n]|$)", Pattern.CASE_INSENSITIVE);
    private Receipts receipts;

    @Override public void onCreate() { super.onCreate(); receipts = new Receipts(this); }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (!getSharedPreferences("payment_sources", MODE_PRIVATE).getBoolean(pkg, false)) return;
        String owner = Receipts.owner();
        if (owner == null) return;
        Notification notification = sbn.getNotification();
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        String title = notification.extras.getCharSequence(Notification.EXTRA_TITLE, "").toString().trim();
        String body = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT,
                notification.extras.getCharSequence(Notification.EXTRA_TEXT, "")).toString().trim();
        if (EXCLUDE.matcher(title + " " + body).find()) return;
        Matcher match = RECEIVED.matcher(body);
        if (!match.find()) {
            match = RECEIVED.matcher(title);
            if (!match.find()) return;
        }
        String amount = match.group(1) == null ? match.group(2) : match.group(1);
        long paise;
        try { paise = new BigDecimal(amount.replace(",", "")).movePointRight(2).longValueExact(); }
        catch (ArithmeticException | NumberFormatException invalid) { return; }
        if (paise <= 0) return;
        Matcher from = FROM.matcher(title + "\n" + body);
        ContentValues data = new ContentValues();
        data.put("owner", owner);
        data.put("event", sbn.getKey() + ":" + (notification.when > 0 ? notification.when : sbn.getPostTime()) + ":" + paise);
        data.put("time", sbn.getPostTime());
        data.put("payer", from.find() ? from.group(1).trim() : "Name unavailable");
        data.put("paise", paise);
        if (receipts.getWritableDatabase().insertWithOnConflict("receipt", null, data, SQLiteDatabase.CONFLICT_IGNORE) == -1) return;
        getContentResolver().notifyChange(Receipts.CHANGES, null);
        if (getSharedPreferences("alerts", MODE_PRIVATE).getBoolean("voice_on", true))
            VoiceSample.announce(this, paise);
    }

    @Override public void onDestroy() { receipts.close(); super.onDestroy(); }
}
