package com.arnav.nimbus;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import top.niunaijun.blackbox.utils.FailureMessage;

/** Apps listed here run as ordinary Android apps in the actual work profile. */
public final class NativeProfileActivity extends BaseActivity {
    private static final int PROVISION = 301;
    private LinearLayout content;
    private boolean managed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        managed = NativeProfileManager.isManaged(this);
        title(managed ? "Nimbus work apps" : "Android work profile", !managed);
        ScrollView scroll = new ScrollView(this);
        content = Ui.column(this); content.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24));
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    @Override protected void onResume() { super.onResume(); render(); }

    private void render() {
        content.removeAllViews();
        LinearLayout info = Ui.card(this); content.addView(info);
        info.addView(Ui.text(this, managed ? "Apps run directly in Android's work profile with separate app data and accounts."
                : "Add one Android work profile for a second installation of your apps. Install apps from that profile's Play Store. Your numbered spaces remain separate.", 16, Ui.TEXT));
        TextView limits = Ui.text(this, "Apps may still reject second installations. This mode does not guarantee acceptance by Swiggy or other apps.", 13, Ui.MUTED);
        Ui.margin(limits, 12, this); info.addView(limits);
        if (!managed) { personalControls(); return; }
        LinearLayout tools = Ui.card(this); content.addView(tools);
        tools.addView(Ui.button(this, "Install apps from Play Store", () -> launchPackage("com.android.vending")));
        tools.addView(Ui.button(this, "Accounts & Google sign-in", () -> open(new Intent(Settings.ACTION_SYNC_SETTINGS))));
        if (NativeProfileManager.ownsProfile(this)) tools.addView(Ui.button(this, "Enable Google components", () -> busy("Enabling Google apps…", () -> NativeProfileManager.enableProfile(this), notes -> {
            if (!notes.isEmpty()) alert("Google apps need attention", notes);
            else toast("Google system apps enabled");
            render();
        })));
        tools.addView(Ui.button(this, "Copy profile report", () -> background(this::report, text -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Nimbus profile report", text)); toast("Profile report copied");
        })));
        LinearLayout apps = Ui.card(this); content.addView(apps);
        apps.addView(Ui.text(this, "Installed work apps", 18, Ui.TEXT));
        background(() -> {
            ArrayList<App> result = new ArrayList<>();
            for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                if (!app.packageName.equals(getPackageName()) && getPackageManager().getLaunchIntentForPackage(app.packageName) != null) result.add(new App(app, app.loadLabel(getPackageManager()).toString()));
            }
            result.sort(Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT))); return result;
        }, list -> {
            // Ignore a stale result if onResume or a repair already replaced this view.
            if (apps.getParent() == null) return;
            if (list.isEmpty()) apps.addView(Ui.text(this, "Install an app from the work Play Store to begin.", 14, Ui.MUTED));
            for (App app : list) {
                LinearLayout row = Ui.row(this); ImageView icon = new ImageView(this);
                try { icon.setImageDrawable(app.info.loadIcon(getPackageManager())); } catch (RuntimeException ignored) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
                row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
                TextView label = Ui.button(this, app.label, () -> launchPackage(app.info.packageName));
                row.addView(label, new LinearLayout.LayoutParams(0, -2, 1)); apps.addView(row);
                row.setOnLongClickListener(v -> { open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.info.packageName))); return true; });
                label.setOnLongClickListener(v -> { open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.info.packageName))); return true; });
            }
        });
    }

    private void personalControls() {
        LinearLayout controls = Ui.card(this); content.addView(controls);
        boolean allowed;
        try { allowed = NativeProfileManager.canCreate(this); } catch (RuntimeException failure) { allowed = false; }
        if (allowed) controls.addView(Ui.button(this, "Create work profile", () -> new AlertDialog.Builder(this)
                .setTitle("Set up an Android work profile?")
                .setMessage("Android will create a separate work profile and make Nimbus its profile manager. You approve this in Android's setup screens. Apps and accounts in existing spaces do not move automatically."
                        + (BuildConfig.HAS_STABLE_SIGNING ? "" : "\n\nThis test build has a temporary signing key. Future builds may require removing the test work profile and its data. Use a test profile until stable signing is configured."))
                .setNegativeButton("Cancel", null).setPositiveButton("Start setup", (d, w) -> {
                    try { startActivityForResult(NativeProfileManager.provisioningIntent(this), PROVISION); }
                    catch (RuntimeException failure) { alert("Setup unavailable", FailureMessage.describe(failure)); }
                }).show()));
        else controls.addView(Ui.text(this, "Android is not allowing a new work profile. A profile may already exist, or this device's policy may prevent creating one.", 14, Ui.MUTED));
        controls.addView(Ui.button(this, "Open Nimbus in work profile", () -> {
            try { if (!NativeProfileManager.openProfile(this)) alert("Open from the Work tab", "Open the briefcase-badged Nimbus app in your launcher's Work tab. Enable the work profile if it is paused."); }
            catch (RuntimeException failure) { alert("Profile unavailable", FailureMessage.describe(failure)); }
        }));
        controls.addView(Ui.button(this, "Android profile settings", () -> open(new Intent(Settings.ACTION_SETTINGS))));
    }

    private void launchPackage(String pkg) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) { alert("App unavailable in this profile", "Install or enable the app in this work profile first. Google apps require a phone with Google services."); return; }
        open(launch);
    }

    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (RuntimeException failure) { alert("Could not open", FailureMessage.describe(failure)); }
    }

    private String report() {
        StringBuilder text = new StringBuilder("Nimbus native work-profile report\nApp: " + BuildConfig.VERSION_NAME + "\nAndroid API: " + Build.VERSION.SDK_INT + "\n");
        text.append("Managed profile: ").append(managed).append("\nProfile owned by Nimbus: ").append(NativeProfileManager.ownsProfile(this)).append("\n");
        text.append("Google enable errors: ").append(getSharedPreferences("NativeProfile", 0).getString("googleErrors", "None recorded")).append("\n");
        for (String pkg : NativeProfileManager.GOOGLE) {
            try { PackageInfo info = getPackageManager().getPackageInfo(pkg, 0); text.append(pkg).append(": ").append(info.versionName).append(" / enabled ").append(info.applicationInfo != null && info.applicationInfo.enabled).append('\n'); }
            catch (PackageManager.NameNotFoundException missing) { text.append(pkg).append(": missing from work profile\n"); }
        }
        return text.toString();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PROVISION) {
            if (result == RESULT_OK) alert("Profile setup complete", "Open Nimbus with the briefcase badge in your launcher's Work tab, then install your apps from its Play Store.");
            else toast("Profile setup did not complete");
        }
    }

    private static final class App {
        final ApplicationInfo info; final String label;
        App(ApplicationInfo info, String label) { this.info = info; this.label = label; }
    }
}
