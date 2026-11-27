package app.aialertpay.mobile;

import android.app.Activity;
import android.os.Bundle;

public final class PlanDetailsActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.plan_details);
        findViewById(R.id.back).setOnClickListener(v -> finish());
    }
}
