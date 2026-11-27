package app.aialertpay.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import java.util.concurrent.TimeUnit;

public final class VerifyActivity extends Activity {
    private FirebaseAuth auth;
    private String number, verificationId;
    private PhoneAuthProvider.ForceResendingToken token;
    private boolean sending, signing;
    private long retryAt;
    private CountDownTimer timer;
    private Button resend, verify;
    private EditText otp;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        number = getIntent().getStringExtra("number");
        if (number == null || !number.matches("[+]91[6-9][0-9]{9}")) {
            finish();
            return;
        }
        setContentView(R.layout.verify);
        resend = findViewById(R.id.resend);
        verify = findViewById(R.id.continue_button);
        status = findViewById(R.id.status);
        otp = findViewById(R.id.otp);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        resend.setOnClickListener(v -> request(true));
        verify.setOnClickListener(v -> {
            String code = otp.getText().toString();
            if (!code.matches("[0-9]{6}")) {
                otp.setError("Enter all 6 digits.");
                return;
            }
            if (verificationId != null) signIn(PhoneAuthProvider.getCredential(verificationId, code));
        });
        if (FirebaseApp.initializeApp(this) == null) {
            status.setText("Phone verification is not configured yet.");
            controls();
            return;
        }
        auth = FirebaseAuth.getInstance();
        if (state != null) {
            verificationId = state.getString("id");
            token = state.getParcelable("token");
            retryAt = state.getLong("retryAt");
            status.setText(state.getString("status"));
        }
        if (verified()) showSuccess();
        else if (state == null || state.getBoolean("sending")) sending = true;
        else controls();
    }

    private boolean verified() {
        return auth != null && auth.getCurrentUser() != null
                && number.equals(auth.getCurrentUser().getPhoneNumber());
    }

    private void controls() {
        if (timer != null) timer.cancel();
        long remaining = retryAt - SystemClock.elapsedRealtime();
        boolean ready = auth != null && !verified() && !signing;
        verify.setEnabled(ready && verificationId != null && !sending);
        otp.setEnabled(ready && verificationId != null && !sending);
        resend.setEnabled(ready && !sending && remaining <= 0);
        resend.setText("Resend code");
        if (remaining > 0 && ready) {
            timer = new CountDownTimer(remaining, 1000) {
                public void onTick(long ms) { resend.setText("Resend code in " + ((ms + 999) / 1000) + "s"); }
                public void onFinish() { controls(); }
            }.start();
        }
    }

    private boolean isTestNumber() {
        return BuildConfig.DEBUG && number.equals(BuildConfig.FIREBASE_TEST_PHONE);
    }

    private void request(boolean force) {
        auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(isTestNumber());
        sending = true;
        status.setText("Requesting verification code…");
        controls();
        PhoneAuthOptions.Builder options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number).setActivity(this).setTimeout(60L, TimeUnit.SECONDS)
                .setCallbacks(callbacks);
        if (force && token != null) options.setForceResendingToken(token);
        PhoneAuthProvider.verifyPhoneNumber(options.build());
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken newToken) {
            verificationId = id;
            token = newToken;
            sending = false;
            retryAt = SystemClock.elapsedRealtime() + 30000;
            if (isTestNumber()) status.setText("Test login ready. Enter the 6-digit code saved in Firebase. No SMS is sent.");
            else status.setText("Code sent to +91 ••••••" + number.substring(number.length() - 4));
            controls();
        }
        @Override public void onVerificationCompleted(PhoneAuthCredential credential) {
            sending = false;
            signIn(credential);
        }
        @Override public void onVerificationFailed(FirebaseException error) {
            sending = false;
            verificationId = null;
            status.setText("Code request failed. " + errorReason(error));
            controls();
        }
    };

    private String errorReason(Exception error) {
        if (error instanceof FirebaseAuthException) return ((FirebaseAuthException) error).getErrorCode();
        if (error instanceof com.google.firebase.FirebaseNetworkException) return "Check your internet connection.";
        if (error instanceof com.google.firebase.FirebaseTooManyRequestsException) return "Too many attempts. Try again later.";
        return error == null ? "Please try again." : error.getClass().getSimpleName();
    }

    private void signIn(PhoneAuthCredential credential) {
        if (signing || verified()) return;
        signing = true;
        status.setText("Verifying…");
        controls();
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) return;
            signing = false;
            if (task.isSuccessful() && verified()) showSuccess();
            else {
                status.setText("Verification failed. " + errorReason(task.getException()));
                controls();
            }
        });
    }

    private void showSuccess() {
        if (isFinishing()) return;
        startActivity(new android.content.Intent(this, SubscriptionActivity.class));
        finish();
    }

    @Override protected void onStart() {
        super.onStart();
        if (auth != null && verified()) showSuccess();
        else if (auth != null && sending) request(false);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("sending", sending);
        out.putString("id", verificationId);
        out.putParcelable("token", token);
        out.putLong("retryAt", retryAt);
        if (status != null) out.putString("status", status.getText().toString());
    }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
