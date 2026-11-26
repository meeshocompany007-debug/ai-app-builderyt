package app.aialertpay.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public final class LoginActivity extends Activity {
    private EditText phone;
    private TextView feedback;
    private Button next;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.login);
        phone = findViewById(R.id.phone);
        feedback = findViewById(R.id.feedback);
        next = findViewById(R.id.continue_button);
        findViewById(R.id.back).setOnClickListener(view -> finish());
        phone.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateForm();
            }
            public void afterTextChanged(Editable value) {}
        });
        next.setOnClickListener(view -> feedback.setText("Number format accepted. OTP verification is not available yet."));
        phone.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) return false;
            if (next.isEnabled()) next.performClick();
            else feedback.setText("Enter a valid 10-digit mobile number.");
            return false;
        });
        updateForm();
    }

    private void updateForm() {
        String number = phone.getText().toString();
        boolean valid = number.matches("[6-9][0-9]{9}");
        next.setEnabled(valid);
        next.setAlpha(valid ? 1f : 0.45f);
        feedback.setText(number.length() == 10 && !valid ? "Enter a valid mobile number starting with 6, 7, 8 or 9." : "");
    }
}
