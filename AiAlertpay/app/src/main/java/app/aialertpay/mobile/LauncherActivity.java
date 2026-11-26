package app.aialertpay.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

public final class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.welcome);
        findViewById(R.id.hero).setClipToOutline(true);
        findViewById(R.id.start).setOnClickListener(view ->
                startActivity(new Intent(this, LoginActivity.class)));
    }
}
