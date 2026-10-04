package com.arnav.nimbus;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class NimbusProfileAdminReceiver extends DeviceAdminReceiver {
    @Override public void onProfileProvisioningComplete(Context context, Intent intent) {
        try { NativeProfileManager.enableProfile(context); }
        catch (RuntimeException failure) { Log.e("NimbusProfile", "Profile setup failed", failure); }
    }
}
