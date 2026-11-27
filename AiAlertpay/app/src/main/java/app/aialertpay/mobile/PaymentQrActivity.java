package app.aialertpay.mobile;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

public final class PaymentQrActivity extends Activity {
    private EditText vpa, amount;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String owner = Receipts.owner();
        address = owner == null ? "" : getSharedPreferences("qr", MODE_PRIVATE).getString(owner, "");
        if (state != null) {
            address = state.getString("address", address);
            value = state.getString("value", "");
        }
        if (state == null ? getIntent().getBooleanExtra(SettingsActivity.EDIT, false) : state.getBoolean("editing")) showEditor();
        else showTab();
    }

    private String address = "", value = "";
    private boolean editing;

    private Bitmap makeQr(String id, String rupees) throws WriterException {
        Uri.Builder payment = new Uri.Builder().scheme("upi").authority("pay")
                .appendQueryParameter("pa", id).appendQueryParameter("pn", id).appendQueryParameter("cu", "INR");
        if (!rupees.isEmpty()) payment.appendQueryParameter("am", rupees);
        return encode(payment.build().toString());
    }

    private void showTab() {
        editing = false;
        setContentView(R.layout.payment_qr);
        findViewById(R.id.qr_back).setOnClickListener(v -> finish());
        findViewById(R.id.open_settings).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
            finish();
        });
        findViewById(R.id.open_team).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, TeamActivity.class));
            finish();
        });
        findViewById(R.id.open_collection).setOnClickListener(v -> finish());
        android.widget.Button edit = findViewById(R.id.edit_qr);
        edit.setOnClickListener(v -> showEditor());
        if (address.isEmpty()) return;
        edit.setText("Edit address or amount");
        ((android.widget.TextView)findViewById(R.id.qr_caption)).setText(address);
        android.widget.TextView hint = findViewById(R.id.qr_hint);
        try {
            Bitmap qr = makeQr(address, value);
            ((ImageView)findViewById(R.id.qr_image)).setImageBitmap(qr);
            hint.setText(value.isEmpty() ? "Scan to pay" : "Scan to pay ₹" + value);
            findViewById(R.id.share_qr).setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.share_qr).setOnClickListener(v -> share(qr));
        } catch (WriterException error) {
            hint.setText("Could not create QR. Check your UPI address.");
        }
    }

    private void showEditor() {
        editing = true;
        setContentView(R.layout.qr_editor);
        vpa = findViewById(R.id.vpa);
        amount = findViewById(R.id.qr_amount);
        vpa.setText(address);
        amount.setText(value);
        findViewById(R.id.qr_back).setOnClickListener(v -> leaveEditor());
        findViewById(R.id.generate_qr).setOnClickListener(v -> generate());
    }

    @Override public void onBackPressed() {
        if (editing) leaveEditor();
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("address", address);
        out.putString("value", value);
        out.putBoolean("editing", editing);
    }

    private void leaveEditor() {
        if (getIntent().getBooleanExtra(SettingsActivity.EDIT, false)) finish();
        else showTab();
    }

    private void generate() {
        String address = vpa.getText().toString().trim();
        String value = amount.getText().toString().trim();
        if (!address.matches("[A-Za-z0-9][A-Za-z0-9._-]*@[A-Za-z0-9][A-Za-z0-9.-]*")) {
            vpa.setError("Enter your full UPI ID, for example name@bank");
            return;
        }
        if (!value.isEmpty() && (!value.matches("[0-9]+(?:\\.[0-9]{1,2})?") || new BigDecimal(value).signum() <= 0)) {
            amount.setError("Enter a positive amount with up to two decimal places");
            return;
        }
        this.address = address;
        this.value = value;
        String owner = Receipts.owner();
        if (owner != null) getSharedPreferences("qr", MODE_PRIVATE).edit().putString(owner, address).apply();
        leaveEditor();
    }

    private Bitmap encode(String payload) throws WriterException {
        int size = 512;
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size);
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) pixels[y * size + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
    }

    private void share(Bitmap bitmap) {
        try {
            File directory = new File(getCacheDir(), "payment_qr");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException();
            File file = File.createTempFile("payment-", ".png", directory);
            try (FileOutputStream output = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException();
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".qrfiles", file);
            Intent send = new Intent(Intent.ACTION_SEND).setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.setClipData(ClipData.newRawUri("Payment QR", uri));
            startActivity(Intent.createChooser(send, "Share payment QR"));
        } catch (IOException error) {
            Toast.makeText(this, "Could not share the QR image.", Toast.LENGTH_LONG).show();
        }
    }
}