package com.arnav.dualspace;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import java.util.*;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.system.user.BUserInfo;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.fake.device.DeviceSpoofManager;
import top.niunaijun.blackbox.utils.FailureMessage;

final class SpaceRepository {
    static final BlackBoxCore CORE = BlackBoxCore.get();
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("DualSpace", 0); }
    static String name(Context c, int id) { return prefs(c).getString("name." + id, "Space " + (id + 1)); }
    static String label(Context c, int id, ApplicationInfo a) {
        return prefs(c).getString("label." + id + "." + a.packageName, a.loadLabel(c.getPackageManager()).toString());
    }
    static boolean hidden(Context c, int id, String pkg) { return prefs(c).getBoolean("hidden." + id + "." + pkg, false); }
    static void checkEngine() {
        if (DualSpaceApplication.engineError != null) throw new IllegalStateException("Virtual engine could not start: " + DualSpaceApplication.engineError);
    }
    static int create(Context c) {
        checkEngine();
        int id = prefs(c).getInt("nextId", 0);
        for (BUserInfo user : CORE.getUsers()) id = Math.max(id, user.id + 1);
        if (CORE.createUser(id) == null) throw new IllegalStateException("Virtual engine is not ready. Retry in a moment.");
        prefs(c).edit().putInt("nextId", id + 1).commit();
        DeviceSpoofManager.values(id);
        return id;
    }
    static void stopSpace(int id) {
        for (ApplicationInfo a : CORE.getInstalledApplications(0, id)) CORE.stopPackage(a.packageName, id);
    }
    static void resetIdentity(int id, boolean random) {
        stopSpace(id);
        if (random) DeviceSpoofManager.generateRandom(id); else DeviceSpoofManager.resetToDefaults(id);
    }
    static void uninstallAll(int id) {
        stopSpace(id);
        for (ApplicationInfo a : CORE.getInstalledApplications(0, id)) CORE.uninstallPackageAsUser(a.packageName, id);
        if (!CORE.getInstalledApplications(0, id).isEmpty()) throw new IllegalStateException("Some apps could not be removed. Retry after restarting the engine.");
    }
    static void delete(Context c, int id) {
        stopSpace(id); CORE.deleteUser(id);
        for (BUserInfo u : CORE.getUsers()) if (u.id == id) throw new IllegalStateException("Space could not be removed");
        DeviceSpoofManager.delete(id);
        SharedPreferences p = prefs(c); SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) {
            if (k.equals("name." + id) || k.equals("note." + id) || k.equals("googleError." + id) || k.startsWith("hidden." + id + ".") || k.startsWith("label." + id + ".")) e.remove(k);
        }
        e.commit();
    }
    static void requireSuccess(InstallResult r) {
        if (r == null || !r.success) throw new IllegalStateException(r == null ? "Engine returned no result" : FailureMessage.orDefault(r.msg, "Installation failed without details"));
    }
    static InstallResult setupGoogle(Context c, int id) {
        InstallResult r;
        try {
            checkEngine();
            r = CORE.installGms(id);
            if (r == null) r = new InstallResult().installError("Engine returned no Google setup result");
        } catch (Exception failure) {
            r = new InstallResult().installError(FailureMessage.describe(failure));
        }
        SharedPreferences.Editor editor = prefs(c).edit();
        if (r.success) editor.remove("googleError." + id);
        else editor.putString("googleError." + id, FailureMessage.orDefault(r.msg, "Google setup failed without details"));
        editor.commit();
        return r;
    }
    static String autoGms(Context c, int id) {
        if (!prefs(c).getBoolean("autoGms", true)) return "";
        if (!CORE.isSupportGms()) return "Google Play services are not installed on this phone. You can import Google APKs from the space menu.";
        InstallResult r = setupGoogle(c, id);
        return r.success ? "" : "Google setup needs attention: " + r.msg + "\nOpen this space's Google Play services menu to retry or copy the setup report.";
    }
    static class AppEntry {
        final ApplicationInfo info; final String label; final Drawable icon;
        AppEntry(Context c, int id, ApplicationInfo a) {
            info = a; label = SpaceRepository.label(c, id, a);
            Drawable d;
            try { d = a.loadIcon(c.getPackageManager()); } catch (Exception e) { d = c.getDrawable(android.R.drawable.sym_def_app_icon); }
            icon = d;
        }
    }
    static class Space {
        final int id; final String name; final List<AppEntry> apps; final boolean google;
        Space(Context c, BUserInfo user) {
            id = user.id; name = SpaceRepository.name(c, id); google = CORE.isInstallGms(id); apps = new ArrayList<>();
            for (ApplicationInfo a : CORE.getInstalledApplications(0, id)) {
                // Service-only Google components stay in the engine, away from the launcher grid.
                if (a.packageName.equals("com.google.android.gms") || a.packageName.equals("com.google.android.gsf") || a.packageName.equals("com.google.android.gsf.login")) continue;
                apps.add(new AppEntry(c, id, a));
            }
            apps.sort(Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT)));
        }
    }
    static List<Space> list(Context c) {
        checkEngine(); List<Space> spaces = new ArrayList<>();
        List<BUserInfo> users = CORE.getUsers();
        for (BUserInfo u : users) spaces.add(new Space(c, u));
        spaces.sort(Comparator.comparingInt(s -> s.id)); return spaces;
    }
}
