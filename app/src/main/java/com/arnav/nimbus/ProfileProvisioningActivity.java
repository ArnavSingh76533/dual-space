package com.arnav.nimbus;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import java.util.ArrayList;

/** Android provisioning callbacks, protected by BIND_DEVICE_ADMIN in the manifest. */
public final class ProfileProvisioningActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String action = getIntent().getAction();
        if (Build.VERSION.SDK_INT >= 29 && DevicePolicyManager.ACTION_GET_PROVISIONING_MODE.equals(action)) {
            int mode = DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE;
            if (Build.VERSION.SDK_INT >= 31) {
                ArrayList<Integer> allowed = getIntent().getIntegerArrayListExtra(DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES);
                if (allowed == null || !allowed.contains(mode)) { setResult(RESULT_CANCELED); finish(); return; }
            }
            setResult(RESULT_OK, new Intent().putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, mode));
        } else if (NativeProfileManager.ownsProfile(this)) {
            try { NativeProfileManager.enableProfile(this); setResult(RESULT_OK); }
            catch (RuntimeException failure) { setResult(RESULT_CANCELED); }
        } else setResult(RESULT_CANCELED);
        finish();
    }
}
