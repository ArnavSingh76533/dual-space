package com.arnav.dualspace;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.util.*;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public final class MainActivity extends BaseActivity {
    static final String REPO = "https://github.com/ArnavSingh76533/dual-space";
    private LinearLayout cards;
    private EditText search;
    private List<SpaceRepository.Space> spaces = new ArrayList<>();
    private boolean showHidden;
    private final Runnable ready = () -> runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) refresh(); });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); title("Dual Space", false);
        icon("⌕", "Search apps", () -> { search.setVisibility(search.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE); search.requestFocus(); });
        icon("⊘", "Ad-free membership", this::membership);
        icon("▦", "QR tools", this::qrTools);
        TextView menu = icon("⋮", "More options", () -> {});
        menu.setOnClickListener(v -> globalMenu(v));
        search = Ui.input(this, "Search all spaces"); search.setVisibility(View.GONE); root.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { render(); }
            public void afterTextChanged(Editable e) {}
        });
        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        cards = Ui.column(this); cards.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 96));
        scroll.addView(cards); content.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        TextView add = Ui.text(this, "+", 38, 0xff12358c); add.setGravity(Gravity.CENTER); add.setBackground(Ui.bg(Ui.ACCENT, 100, this));
        add.setContentDescription("Create a new space"); add.setOnClickListener(v -> create());
        FrameLayout.LayoutParams fab = new FrameLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64), Gravity.BOTTOM | Gravity.END);
        fab.setMargins(0, 0, Ui.dp(this, 20), Ui.dp(this, 20)); content.addView(add, fab);
        showHidden = SpaceRepository.prefs(this).getBoolean("showHidden", false);
        if (state != null) search.setText(state.getString("search", ""));
        if (DualSpaceApplication.engineError == null) BlackBoxCore.get().addServiceAvailableCallback(ready);
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("search", search.getText().toString()); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { BlackBoxCore.get().removeServiceAvailableCallback(ready); super.onDestroy(); }
    private void refresh() {
        background(() -> SpaceRepository.list(this), result -> { spaces = result; render(); });
    }
    private void render() {
        if (cards == null) return;
        cards.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (spaces.isEmpty()) {
            TextView empty = Ui.text(this, "Your second space starts here", 24, Ui.TEXT); Ui.bold(empty); Ui.margin(empty, 80, this); cards.addView(empty);
            TextView help = Ui.text(this, "Tap + to create a space, then choose apps to clone. Each space keeps its own logins and data.", 16, Ui.MUTED); Ui.margin(help, 16, this); cards.addView(help);
            return;
        }
        for (SpaceRepository.Space s : spaces) {
            List<SpaceRepository.AppEntry> visible = new ArrayList<>();
            for (SpaceRepository.AppEntry a : s.apps) {
                if (!showHidden && SpaceRepository.hidden(this, s.id, a.info.packageName)) continue;
                if (!query.isEmpty() && !a.label.toLowerCase(Locale.ROOT).contains(query) && !a.info.packageName.toLowerCase(Locale.ROOT).contains(query)) continue;
                visible.add(a);
            }
            if (!query.isEmpty() && visible.isEmpty() && !s.name.toLowerCase(Locale.ROOT).contains(query)) continue;
            LinearLayout card = Ui.card(this); cards.addView(card);
            LinearLayout header = Ui.row(this); card.addView(header);
            TextView number = Ui.text(this, Integer.toString(s.id + 1), 14, 0xffdce4ff); Ui.bold(number); number.setGravity(Gravity.CENTER);
            number.setBackground(Ui.bg(Ui.BLUE, 10, this)); header.addView(number, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 28)));
            TextView label = Ui.text(this, s.name, 14, Ui.MUTED); label.setPadding(Ui.dp(this, 10), 0, 0, 0);
            header.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            if (s.google) { TextView g = Ui.text(this, "G", 12, Ui.ACCENT); g.setContentDescription("Google stack installed"); header.addView(g); }
            TextView more = Ui.text(this, "⋮", 24, Ui.TEXT); more.setGravity(Gravity.CENTER); more.setContentDescription("Options for " + s.name);
            header.addView(more, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40))); more.setOnClickListener(v -> spaceMenu(v, s));
            String note = SpaceRepository.prefs(this).getString("note." + s.id, "");
            if (!note.isEmpty()) { TextView n = Ui.text(this, note, 12, Ui.MUTED); n.setMaxLines(2); card.addView(n); }
            if (visible.isEmpty()) { card.addView(Ui.button(this, "+  Add apps", () -> install(s.id))); continue; }
            GridLayout grid = new GridLayout(this); grid.setColumnCount(4); card.addView(grid);
            for (SpaceRepository.AppEntry a : visible) {
                LinearLayout tile = Ui.column(this); tile.setGravity(Gravity.CENTER); tile.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 10));
                GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width = 0; p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); tile.setLayoutParams(p);
                ImageView image = new ImageView(this); image.setImageDrawable(a.icon); tile.addView(image, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46)));
                TextView text = Ui.text(this, a.label, 12, Ui.TEXT); text.setSingleLine(true); text.setEllipsize(android.text.TextUtils.TruncateAt.END); text.setGravity(Gravity.CENTER); Ui.margin(text, 6, this); tile.addView(text);
                if (SpaceRepository.hidden(this, s.id, a.info.packageName)) tile.setAlpha(.5f);
                tile.setContentDescription(a.label + " in " + s.name); tile.setOnClickListener(v -> launch(s.id, a.info.packageName));
                tile.setOnLongClickListener(v -> { appMenu(tile, s.id, a); return true; }); grid.addView(tile);
            }
        }
        if (cards.getChildCount() == 0) cards.addView(Ui.text(this, "No matching apps", 18, Ui.MUTED));
    }
    private void create() {
        busy("Creating space…", () -> SpaceRepository.create(this), id -> { refresh(); install(id); });
    }
    private void install(int id) { startActivity(new Intent(this, InstallActivity.class).putExtra("space", id)); }
    private void launch(int id, String pkg) {
        busy("Opening app…", () -> { SpaceRepository.checkEngine(); return BlackBoxCore.get().launchApk(pkg, id); }, ok -> {
            if (!ok) alert("Could not launch", "The app may be a background service or incompatible with this Android version. Check Google services from the space menu.");
        });
    }
    private void spaceMenu(View anchor, SpaceRepository.Space s) {
        PopupMenu p = new PopupMenu(this, anchor);
        String[] names = {"Install App", "Note", "Reset Device / Device identity", "Google Play services", "Rename space", "Uninstall all apps", "Delete space"};
        for (int i = 0; i < names.length; i++) p.getMenu().add(0, i, i, names[i]);
        p.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 0: install(s.id); break;
                case 1: edit("Note", "note." + s.id, "", true); break;
                case 2: startActivity(new Intent(this, IdentityActivity.class).putExtra("space", s.id)); break;
                case 3: google(s.id); break;
                case 4: edit("Rename space", "name." + s.id, s.name, false); break;
                case 5: confirm("Uninstall all apps?", "Remove apps and their data only from " + s.name + ", including Google accounts in this space.", () -> busy("Removing apps…", () -> { SpaceRepository.uninstallAll(s.id); return true; }, ok -> refresh())); break;
                case 6: confirm("Delete " + s.name + "?", "Permanently remove this space and all of its app data.", () -> busy("Deleting space…", () -> { SpaceRepository.delete(this, s.id); return true; }, ok -> refresh())); break;
            }
            return true;
        }); p.show();
    }
    private void edit(String title, String key, String fallback, boolean multiline) {
        EditText v = Ui.input(this, title); v.setText(SpaceRepository.prefs(this).getString(key, fallback));
        if (multiline) { v.setSingleLine(false); v.setMinLines(3); v.setMaxLines(8); v.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE); }
        new AlertDialog.Builder(this).setTitle(title).setView(v).setNegativeButton("Cancel", null).setPositiveButton("Save", (d, w) -> {
            String value = v.getText().toString().trim();
            if (!multiline && value.isEmpty()) SpaceRepository.prefs(this).edit().remove(key).apply();
            else SpaceRepository.prefs(this).edit().putString(key, value).apply(); refresh();
        }).show();
    }
    private void appMenu(View anchor, int id, SpaceRepository.AppEntry a) {
        PopupMenu p = new PopupMenu(this, anchor);
        boolean hidden = SpaceRepository.hidden(this, id, a.info.packageName);
        String[] names = {"Open", "Rename", hidden ? "Unhide" : "Hide", "Create shortcut", "Force stop", "Clear app data", "Uninstall"};
        for (int i = 0; i < names.length; i++) p.getMenu().add(0, i, i, names[i]);
        p.setOnMenuItemClickListener(item -> {
            String pkg = a.info.packageName;
            switch (item.getItemId()) {
                case 0: launch(id, pkg); break;
                case 1: edit("Rename app", "label." + id + "." + pkg, a.label, false); break;
                case 2: SpaceRepository.prefs(this).edit().putBoolean("hidden." + id + "." + pkg, !hidden).apply(); render(); break;
                case 3: shortcut(id, a); break;
                case 4: busy("Stopping…", () -> { BlackBoxCore.get().stopPackage(pkg, id); return true; }, ok -> toast("App stopped")); break;
                case 5: confirm("Clear " + a.label + " data?", "Reset this app's login, settings and files in " + SpaceRepository.name(this, id) + ".", () -> busy("Clearing data…", () -> { BlackBoxCore.get().stopPackage(pkg, id); BlackBoxCore.get().clearPackage(pkg, id); return true; }, ok -> refresh())); break;
                case 6: confirm("Uninstall " + a.label + "?", "Remove the app and its data from this space.", () -> busy("Uninstalling…", () -> { BlackBoxCore.get().uninstallPackageAsUser(pkg, id); if (BlackBoxCore.get().isInstalled(pkg, id)) throw new IllegalStateException("Uninstall failed"); return true; }, ok -> refresh())); break;
            } return true;
        }); p.show();
    }
    private void shortcut(int id, SpaceRepository.AppEntry a) {
        if (android.os.Build.VERSION.SDK_INT < 26) { toast("Pinned shortcuts require Android 8 or later"); return; }
        ShortcutManager manager = getSystemService(ShortcutManager.class);
        if (manager == null || !manager.isRequestPinShortcutSupported()) { toast("Your launcher does not support pinned shortcuts"); return; }
        Bitmap bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
        android.graphics.drawable.Drawable d = a.icon.mutate(); d.setBounds(0, 0, 192, 192); d.draw(new Canvas(bitmap));
        Intent launch = new Intent(this, ShortcutActivity.class).setAction(Intent.ACTION_VIEW).putExtra("space", id).putExtra("package", a.info.packageName);
        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, id + ":" + a.info.packageName).setShortLabel(a.label + " · " + (id + 1)).setIcon(Icon.createWithBitmap(bitmap)).setIntent(launch).build();
        manager.requestPinShortcut(shortcut, null);
    }
    private void globalMenu(View anchor) {
        PopupMenu p = new PopupMenu(this, anchor);
        String[] names = {"Settings", showHidden ? "Hide hidden apps" : "Show hidden apps", "Restart Engine", "Membership & subscription", "Share"};
        for (int i = 0; i < names.length; i++) p.getMenu().add(0, i, i, names[i]);
        p.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 0: startActivity(new Intent(this, SettingsActivity.class)); break;
                case 1: showHidden = !showHidden; SpaceRepository.prefs(this).edit().putBoolean("showHidden", showHidden).apply(); render(); break;
                case 2: busy("Stopping virtual apps…", () -> { for (SpaceRepository.Space s : SpaceRepository.list(this)) SpaceRepository.stopSpace(s.id); return true; }, ok -> restart()); break;
                case 3: membership(); break;
                case 4: startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Dual Space — separate apps, accounts and spaces.\n" + REPO), "Share Dual Space")); break;
            } return true;
        }); p.show();
    }
    private void restart() {
        Intent launch = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.AlarmManager alarm = getSystemService(android.app.AlarmManager.class);
        alarm.set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 800, pending);
        for (android.app.ActivityManager.RunningAppProcessInfo process : getSystemService(android.app.ActivityManager.class).getRunningAppProcesses()) {
            if (process.uid == android.os.Process.myUid() && process.pid != android.os.Process.myPid()) android.os.Process.killProcess(process.pid);
        }
        finishAffinity(); android.os.Process.killProcess(android.os.Process.myPid());
    }
    private void membership() { alert("All features included", "Dual Space is free and ad-free. Multiple spaces, device identities and Google service setup are included. There is no subscription."); }
    private void google(int id) {
        background(() -> GoogleDiagnostics.status(id), state -> new AlertDialog.Builder(this).setTitle("Google Play services")
                .setMessage(state + "Setup uses the Google apps already installed on your phone. App compatibility and sign-in vary by device.")
                .setNegativeButton("Close", null).setNeutralButton("More", (d, w) -> new AlertDialog.Builder(this)
                        .setTitle("Google services tools").setItems(new String[]{"Open Play Store", "Copy setup report"}, (tools, which) -> {
                            if (which == 0) launch(id, "com.android.vending");
                            else background(() -> GoogleDiagnostics.report(this, id), this::copyGoogleReport);
                        }).show())
                .setPositiveButton("Set up / repair", (d, w) -> setupGoogle(id)).show());
    }
    private void setupGoogle(int id) {
        busy("Setting up Google…", () -> {
            InstallResult result = SpaceRepository.setupGoogle(this, id);
            return new GoogleAttempt(result, result.success ? "" : GoogleDiagnostics.report(this, id));
        }, attempt -> {
            refresh();
            if (attempt.result.success) google(id);
            else new AlertDialog.Builder(this).setTitle("Google setup needs attention")
                    .setMessage(attempt.result.msg + "\n\nCompleted components are kept. Restart the engine and retry. If it still fails, copy the setup report to troubleshoot this device.")
                    .setNegativeButton("Close", null).setNeutralButton("Copy report", (d, w) -> copyGoogleReport(attempt.report))
                    .setPositiveButton("Retry", (d, w) -> setupGoogle(id)).show();
        });
    }
    private void copyGoogleReport(String report) {
        android.content.ClipboardManager clipboard = getSystemService(android.content.ClipboardManager.class);
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Google setup report", report));
        toast("Setup report copied");
    }
    private static final class GoogleAttempt {
        final InstallResult result;
        final String report;
        GoogleAttempt(InstallResult result, String report) { this.result = result; this.report = report; }
    }
    private void qrTools() {
        new AlertDialog.Builder(this).setTitle("QR tools").setItems(new String[]{"Scan an app package", "Show download QR"}, (d, which) -> {
            if (which == 0) new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setPrompt("Scan an installed app's package name or Play Store link").setBeepEnabled(false).initiateScan();
            else {
                try {
                    Bitmap qr = new BarcodeEncoder().createBitmap(new MultiFormatWriter().encode(REPO, BarcodeFormat.QR_CODE, 600, 600));
                    ImageView image = new ImageView(this); image.setImageBitmap(qr); image.setAdjustViewBounds(true); image.setContentDescription("Download Dual Space from GitHub");
                    new AlertDialog.Builder(this).setTitle("Download Dual Space").setView(image).setPositiveButton("Close", null).show();
                } catch (Exception e) { alert("QR error", e.getMessage()); }
            }
        }).show();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        IntentResult scanned = IntentIntegrator.parseActivityResult(request, result, data);
        if (scanned == null) { super.onActivityResult(request, result, data); return; }
        if (scanned.getContents() == null) return;
        String text = scanned.getContents().trim();
        android.net.Uri uri = android.net.Uri.parse(text);
        String pkg = text;
        if (uri.isHierarchical() && ("play.google.com".equals(uri.getHost()) || "market".equals(uri.getScheme()))) pkg = uri.getQueryParameter("id");
        if (pkg == null || !pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")) { alert("App package required", "Use a package name such as org.telegram.messenger, or a Play Store app link."); return; }
        if (spaces.isEmpty()) { toast("Create a space first"); return; }
        String scannedPackage = pkg;
        new AlertDialog.Builder(this).setTitle("Clone " + pkg).setItems(spaces.stream().map(s -> s.name).toArray(String[]::new), (d, w) -> {
            startActivity(new Intent(this, InstallActivity.class).putExtra("space", spaces.get(w).id).putExtra("query", scannedPackage));
        }).show();
    }
}
