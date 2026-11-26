package app.aialertpay.mobile;

import android.app.Activity;
import android.os.Bundle;

public final class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // The launcher icon is the only approved milestone so far.
        finish();
    }
}
