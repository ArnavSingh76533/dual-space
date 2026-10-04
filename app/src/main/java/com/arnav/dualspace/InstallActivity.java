package com.arnav.dualspace;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public final class InstallActivity extends BaseActivity {
    private int space;
    private boolean system;
    private EditText search;
    private LinearLayout results;
    private TextView installButton;
    private final Set<String> selected = new LinkedHashSet<>();
    private List<SpaceRepository.AppEntry> entries = new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); space = getIntent().getIntExtra("space", 0);
        title("Install Applications", true);
        TextView description = Ui.text(this, SpaceRepository.name(this, space), 13, Ui.MUTED); description.setPadding(Ui.dp(this, 20), 0, 0, Ui.dp(this, 8)); root.addView(description);
        LinearLayout tabs = Ui.row(this);
        TextView user = Ui.button(this, "Installed Apps", () -> { system = false; render(); });
        TextView sys = Ui.button(this, "System Apps", () -> { system = true; render(); });
        tabs.addView(user, new LinearLayout.LayoutParams(0, -2, 1)); tabs.addView(sys, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(tabs);
        search = Ui.input(this, "Search app name or package"); root.addView(search);
        search.setText(state == null ? getIntent().getStringExtra("query") : state.getString("query", ""));
        if (state != null) { system = state.getBoolean("system"); ArrayList<String> saved = state.getStringArrayList("selected"); if (saved != null) selected.addAll(saved); }
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { render(); }
            public void afterTextChanged(Editable e) {}
        });
        ScrollView scroll = new ScrollView(this); results = Ui.column(this); results.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 16)); scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = Ui.row(this); actions.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        actions.addView(Ui.button(this, "Import APK / APKS", this::importApk), new LinearLayout.LayoutParams(0, -2, 1));
        installButton = Ui.button(this, "Clone (0)", this::cloneSelected); actions.addView(installButton, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(actions);
        results.addView(Ui.text(this, "Loading apps…", 16, Ui.MUTED));
        background(() -> {
            List<SpaceRepository.AppEntry> list = new ArrayList<>();
            for (ApplicationInfo a : getPackageManager().getInstalledApplications(0)) {
                if (!a.packageName.equals(getPackageName())) list.add(new SpaceRepository.AppEntry(this, -1, a));
            }
            list.sort(Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT))); return list;
        }, list -> { entries = list; render(); });
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("space", space); out.putString("query", search.getText().toString()); out.putBoolean("system", system);
        out.putStringArrayList("selected", new ArrayList<>(selected)); super.onSaveInstanceState(out);
    }
    private void render() {
        if (results == null) return;
        results.removeAllViews(); String query = search.getText().toString().toLowerCase(Locale.ROOT);
        List<SpaceRepository.AppEntry> filtered = new ArrayList<>();
        for (SpaceRepository.AppEntry e : entries) {
            boolean isSystem = (e.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (isSystem != system) continue;
            if (e.label.toLowerCase(Locale.ROOT).contains(query) || e.info.packageName.toLowerCase(Locale.ROOT).contains(query)) filtered.add(e);
        }
        // Switch tabs automatically for an exact package scanned from a QR.
        if (filtered.isEmpty() && getIntent().hasExtra("query") && !query.isEmpty()) {
            for (SpaceRepository.AppEntry e : entries) if (e.info.packageName.equals(query)) { system = (e.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0; filtered.add(e); break; }
        }
        String last = ""; GridLayout grid = null;
        for (SpaceRepository.AppEntry e : filtered) {
            String letter = e.label.isEmpty() ? "#" : e.label.substring(0, 1).toUpperCase(Locale.ROOT);
            if (!letter.matches("[A-Z]")) letter = "#";
            if (!letter.equals(last)) {
                TextView section = Ui.text(this, letter, 14, Ui.ACCENT); Ui.margin(section, 16, this); results.addView(section);
                grid = new GridLayout(this); grid.setColumnCount(4); results.addView(grid); last = letter;
            }
            LinearLayout tile = Ui.column(this); tile.setGravity(Gravity.CENTER); tile.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 8));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width = 0; p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); tile.setLayoutParams(p);
            ImageView icon = new ImageView(this); icon.setImageDrawable(e.icon); tile.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
            TextView label = Ui.text(this, e.label, 11, Ui.TEXT); label.setGravity(Gravity.CENTER); label.setMaxLines(2); Ui.margin(label, 5, this); tile.addView(label);
            if (selected.contains(e.info.packageName)) tile.setBackground(Ui.bg(0xff304070, 10, this));
            tile.setContentDescription(e.label + (selected.contains(e.info.packageName) ? ", selected" : ", not selected"));
            tile.setOnClickListener(v -> { if (!selected.add(e.info.packageName)) selected.remove(e.info.packageName); render(); });
            tile.setOnLongClickListener(v -> { alert(e.label, e.info.packageName); return true; }); grid.addView(tile);
        }
        if (filtered.isEmpty()) results.addView(Ui.text(this, "No matching apps", 16, Ui.MUTED));
        if (installButton != null) installButton.setText("Clone (" + selected.size() + ")");
    }
    private void cloneSelected() {
        if (selected.isEmpty()) { toast("Select one or more apps"); return; }
        List<String> packages = new ArrayList<>(selected);
        busy("Installing " + packages.size() + " apps…", () -> {
            SpaceRepository.checkEngine();
            StringBuilder report = new StringBuilder();
            String google = SpaceRepository.autoGms(this, space);
            if (!google.isEmpty()) report.append(google).append("\n\n");
            for (String pkg : packages) {
                InstallResult r = BlackBoxCore.get().installPackageAsUser(pkg, space);
                if (!r.success || !BlackBoxCore.get().isInstalled(pkg, space)) report.append(pkg).append(": ").append(r.msg == null ? "Installation failed" : r.msg).append("\n");
            }
            return report.toString();
        }, report -> {
            if (report.isEmpty()) { toast("Apps installed in " + SpaceRepository.name(this, space)); finish(); }
            else new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Installation results").setMessage(report).setPositiveButton("Done", (d, w) -> finish()).show();
        });
    }
    private void importApk() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, 42);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 42 || result != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        for (Uri uri : uris) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (SecurityException ignored) { }
        }
        busy("Importing APKs…", () -> {
            StringBuilder report = new StringBuilder(SpaceRepository.autoGms(this, space));
            for (Uri uri : uris) {
                try {
                    SpaceRepository.requireSuccess(BlackBoxCore.get().installPackageAsUser(uri, space));
                } finally {
                    try { getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (SecurityException ignored) { }
                }
            }
            return report.toString();
        }, report -> { if (!report.isEmpty()) alert("Google setup", report); else toast("Import complete"); if (report.isEmpty()) finish(); });
    }
}
