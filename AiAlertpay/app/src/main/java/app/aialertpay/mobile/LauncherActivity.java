package app.aialertpay.mobile;

import android.app.Activity;
import android.os.Bundle;

public final class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.welcome);
        findViewById(R.id.hero).setClipToOutline(true);
    }
}
