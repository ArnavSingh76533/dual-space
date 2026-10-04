package com.arnav.dualspace;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;

public final class SettingsActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); title("Settings", true);
        ScrollView scroll = new ScrollView(this); LinearLayout list = Ui.column(this);
        list.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24)); scroll.addView(list); root.addView(scroll);
        LinearLayout general = Ui.card(this); list.addView(general);
        toggle(general, "Set up Google services automatically", "autoGms", true);
        toggle(general, "Keep the engine running in background", "background", true);
        TextView hint = Ui.text(this, "Restart Engine after changing the background setting.", 12, Ui.MUTED); general.addView(hint);
        LinearLayout permissions = Ui.card(this); list.addView(permissions);
        permissions.addView(Ui.button(this, "App permissions", this::permissions));
        permissions.addView(Ui.button(this, "Storage access", this::storage));
        permissions.addView(Ui.button(this, "Battery settings", () -> open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))));
        permissions.addView(Ui.button(this, "Android app settings", () -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + getPackageName())))));
        LinearLayout about = Ui.card(this); list.addView(about);
        TextView version = Ui.text(this, "Dual Space 1.0.0", 19, Ui.TEXT); Ui.bold(version); about.addView(version);
        TextView detail = Ui.text(this, "Multiple spaces • Separate app data • No subscription\n\nBased on the Apache-licensed BlackBox engine and Black00Z's modern Android fork. Google APKs are imported from your phone.\n\nThe engine uses legacy Android APIs. Android 14–16 and individual apps need device testing. Google login, notifications and Play Integrity-dependent apps may not work in a virtual space.", 14, Ui.MUTED);
        Ui.margin(detail, 12, this); about.addView(detail);
        about.addView(Ui.button(this, "Source code & build downloads", () -> open(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(MainActivity.REPO)))));
    }
    private void toggle(LinearLayout list, String label, String key, boolean fallback) {
        androidx.appcompat.widget.SwitchCompat v = new androidx.appcompat.widget.SwitchCompat(this);
        v.setText(label); v.setTextColor(Ui.TEXT); v.setTextSize(15); v.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        v.setChecked(SpaceRepository.prefs(this).getBoolean(key, fallback));
        v.setOnCheckedChangeListener((w, checked) -> SpaceRepository.prefs(this).edit().putBoolean(key, checked).apply()); list.addView(v);
    }
    private void permissions() {
        String[] permissions = {android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_CONTACTS};
        String[] labels = {"Camera", "Microphone", "Location", "Read shared storage", "Write shared storage", "Contacts"};
        boolean[] chosen = new boolean[permissions.length];
        new AlertDialog.Builder(this).setTitle("Permissions for cloned apps").setMultiChoiceItems(labels, chosen, (d, w, yes) -> chosen[w] = yes)
                .setNegativeButton("Cancel", null).setPositiveButton("Request", (d, w) -> {
                    java.util.ArrayList<String> requested = new java.util.ArrayList<>();
                    for (int i = 0; i < chosen.length; i++) if (chosen[i]) requested.add(permissions[i]);
                    if (!requested.isEmpty()) requestPermissions(requested.toArray(new String[0]), 77);
                }).show();
    }
    private void storage() {
        if (android.os.Build.VERSION.SDK_INT >= 30) open(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, android.net.Uri.parse("package:" + getPackageName())));
        else requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 78);
    }
    private void open(Intent i) {
        try { startActivity(i); } catch (android.content.ActivityNotFoundException e) { toast("This settings page is unavailable on your phone"); }
    }
}
