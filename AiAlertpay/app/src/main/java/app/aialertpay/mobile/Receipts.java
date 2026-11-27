package app.aialertpay.mobile;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import com.google.firebase.auth.FirebaseAuth;

final class Receipts extends SQLiteOpenHelper {
    static final Uri CHANGES = Uri.parse("content://app.aialertpay.mobile/receipts");
    Receipts(Context context) { super(context, "receipts.db", null, 1); }

    static String owner() {
        try { return FirebaseAuth.getInstance().getUid(); }
        catch (IllegalStateException unconfigured) { return null; }
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE receipt (owner TEXT NOT NULL, event TEXT NOT NULL, time INTEGER NOT NULL, payer TEXT NOT NULL, paise INTEGER NOT NULL, PRIMARY KEY(owner,event))");
        db.execSQL("CREATE INDEX receipt_day ON receipt(owner,time)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unsupported receipt schema");
    }
}
