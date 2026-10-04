package com.arnav.nimbus;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Build;
import top.niunaijun.blackbox.utils.FailureMessage;

/** Android-owned work profile; no virtual package manager, identity hooks or APK rewriting. */
final class NativeProfileManager {
    static final String[] GOOGLE = {"com.google.android.gsf", "com.google.android.gms", "com.android.vending"};
    private NativeProfileManager() { }

    static boolean isManaged(Context context) {
        UserManager users = context.getSystemService(UserManager.class);
        if (Build.VERSION.SDK_INT >= 30) return users != null && users.isManagedProfile();
        // Before API 30, the public owner check identifies profiles provisioned by Nimbus.
        DevicePolicyManager policies = context.getSystemService(DevicePolicyManager.class);
        return policies != null && policies.isProfileOwnerApp(context.getPackageName());
    }

    static ComponentName admin(Context context) { return new ComponentName(context, NimbusProfileAdminReceiver.class); }

    static boolean ownsProfile(Context context) {
        DevicePolicyManager policies = context.getSystemService(DevicePolicyManager.class);
        return isManaged(context) && policies != null && policies.isProfileOwnerApp(context.getPackageName());
    }

    static boolean canCreate(Context context) {
        DevicePolicyManager policies = context.getSystemService(DevicePolicyManager.class);
        return !isManaged(context) && policies != null
                && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)
                && policies.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
    }

    static Intent provisioningIntent(Context context) {
        return new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin(context));
    }

    static String enableProfile(Context context) {
        if (!ownsProfile(context)) throw new IllegalStateException("Nimbus is not the owner of this work profile");
        DevicePolicyManager policies = context.getSystemService(DevicePolicyManager.class);
        ComponentName admin = admin(context);
        policies.setProfileName(admin, "Nimbus");
        StringBuilder notes = new StringBuilder();
        for (String pkg : GOOGLE) {
            try { policies.enableSystemApp(admin, pkg); }
            catch (RuntimeException failure) { notes.append(pkg).append(": ").append(FailureMessage.describe(failure)).append('\n'); }
        }
        policies.setProfileEnabled(admin);
        context.getSharedPreferences("NativeProfile", 0).edit().putString("googleErrors", notes.toString()).apply();
        return notes.toString();
    }

    static boolean openProfile(Context context) {
        LauncherApps launcher = context.getSystemService(LauncherApps.class);
        if (launcher == null) return false;
        UserHandle current = android.os.Process.myUserHandle();
        UserManager users = context.getSystemService(UserManager.class);
        if (users == null) return false;
        for (UserHandle profile : users.getUserProfiles()) {
            if (current.equals(profile)) continue;
            for (LauncherActivityInfo activity : launcher.getActivityList(context.getPackageName(), profile)) {
                launcher.startMainActivity(activity.getComponentName(), profile, null, null);
                return true;
            }
        }
        return false;
    }
}
