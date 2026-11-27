package app.aialertpay.mobile;

import android.app.Activity;
import android.app.AlertDialog;
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
        setContentView(R.layout.payment_qr);
        vpa = findViewById(R.id.vpa);
        amount = findViewById(R.id.qr_amount);
        String owner = Receipts.owner();
        if (owner != null) vpa.setText(getSharedPreferences("qr", MODE_PRIVATE).getString(owner, ""));
        findViewById(R.id.qr_back).setOnClickListener(v -> finish());
        findViewById(R.id.generate_qr).setOnClickListener(v -> generate());
    }

    private void generate() {
        String address = vpa.getText().toString().trim();
        String value = amount.getText().toString().trim();
        if (!address.matches("[A-Za-z0-9][A-Za-z0-9._-]*@[A-Za-z0-9][A-Za-z0-9.-]*")) {
            vpa.setError("Enter your full UPI ID, for example name@bank");
            return;
        }
        Uri.Builder payment = new Uri.Builder().scheme("upi").authority("pay")
                .appendQueryParameter("pa", address).appendQueryParameter("pn", address)
                .appendQueryParameter("cu", "INR");
        if (!value.isEmpty()) {
            if (!value.matches("[0-9]+(?:\\.[0-9]{1,2})?") || new BigDecimal(value).signum() <= 0) {
                amount.setError("Enter a positive amount with up to two decimal places");
                return;
            }
            payment.appendQueryParameter("am", new BigDecimal(value).toPlainString());
        }
        try {
            Bitmap bitmap = encode(payment.build().toString());
            String owner = Receipts.owner();
            if (owner != null) getSharedPreferences("qr", MODE_PRIVATE).edit().putString(owner, address).apply();
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setAdjustViewBounds(true);
            image.setContentDescription("Payment QR for " + address);
            new AlertDialog.Builder(this).setTitle("Scan to pay")
                    .setMessage(address + (value.isEmpty() ? "" : "  ·  ₹" + value)).setView(image)
                    .setNegativeButton("Done", null).setPositiveButton("Share QR", (d, w) -> share(bitmap)).show();
        } catch (WriterException error) {
            Toast.makeText(this, "Could not create QR. Check the UPI ID.", Toast.LENGTH_LONG).show();
        }
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