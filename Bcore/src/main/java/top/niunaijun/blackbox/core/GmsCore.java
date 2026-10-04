package top.niunaijun.blackbox.core;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.fake.frameworks.BPackageManager;
import top.niunaijun.blackbox.utils.FailureMessage;


public class GmsCore {
    private static final String TAG = "GmsCore";

    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();
    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";

    static {
        GOOGLE_APP.add(VENDING_PKG);
        GOOGLE_APP.add("com.google.android.play.games");
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");

        
        GOOGLE_SERVICE.add(GMS_PKG);
        GOOGLE_SERVICE.add(GSF_PKG);
        GOOGLE_SERVICE.add("com.google.android.gsf.login");
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
    }

    public static boolean isGoogleService(String packageName) {
        return GOOGLE_SERVICE.contains(packageName);
    }

    public static boolean isGoogleAppOrService(String str) {
        return GOOGLE_APP.contains(str) || GOOGLE_SERVICE.contains(str);
    }

    private static InstallResult installPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            if (blackBoxCore.isInstalled(packageName, userId)) {
                continue;
            }
            try {
                BlackBoxCore.getContext().getPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                
                continue;
            }
            InstallResult installResult = blackBoxCore.installPackageAsUser(packageName, userId);
            if (!installResult.success) {
                return installResult;
            }
        }
        return new InstallResult();
    }

    private static void uninstallPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            blackBoxCore.uninstallPackageAsUser(packageName, userId);
        }
    }

    public static InstallResult installGApps(int userId) {
        // Install in dependency order and preserve completed dependencies for a retry.
        String[] required = {GSF_PKG, GMS_PKG, VENDING_PKG};
        for (String pkg : required) {
            try {
                BlackBoxCore.getPackageManager().getApplicationInfo(pkg, 0);
            } catch (PackageManager.NameNotFoundException e) {
                return new InstallResult().installError(pkg, "Not installed on this phone: " + pkg);
            }
        }
        ArrayList<String> ordered = new ArrayList<>();
        ordered.add(GSF_PKG);
        try {
            BlackBoxCore.getPackageManager().getApplicationInfo("com.google.android.gsf.login", 0);
            ordered.add("com.google.android.gsf.login");
        } catch (PackageManager.NameNotFoundException ignored) { }
        ordered.add(GMS_PKG);
        ordered.add(VENDING_PKG);
        try {
            GoogleSetupRunner.run(ordered.toArray(new String[0]), new GoogleSetupRunner.Backend() {
                @Override public boolean isInstalled(String pkg) throws Exception {
                    return BPackageManager.get().isInstalledInSpace(pkg, userId);
                }
                @Override public void install(String pkg) {
                    InstallResult result = BlackBoxCore.get().installPackageAsUser(pkg, userId);
                    if (result == null || !result.success) {
                        throw new IllegalStateException(result == null ? "Engine returned no installation result" : FailureMessage.orDefault(result.msg, "Installer reported a failure without details"));
                    }
                }
            });
        } catch (GoogleSetupRunner.SetupFailure failure) {
            return new InstallResult().installError(failure.packageName, failure.getMessage());
        }
        return new InstallResult();
    }

    public static void uninstallGApps(int userId) {
        uninstallPackages(GOOGLE_SERVICE, userId);
        uninstallPackages(GOOGLE_APP, userId);
    }

    public static boolean hasGmsTraces(int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();

        Set<String> known = new HashSet<>();
        known.addAll(GOOGLE_SERVICE);
        known.addAll(GOOGLE_APP);

        for (String pkg : known) {
            try {
                if (blackBoxCore.isInstalled(pkg, userId)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            List<PackageInfo> installed = blackBoxCore.getInstalledPackages(0, userId);
            for (PackageInfo pi : installed) {
                if (pi == null || pi.packageName == null) {
                    continue;
                }
                String pkg = pi.packageName;
                if (VENDING_PKG.equals(pkg) || pkg.startsWith("com.google.android.")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Best-effort cleanup: clears package data and uninstalls known Google packages
     * for the given virtual user. Intended for "wipe" UX.
     */
    public static void wipeGApps(int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();

        Set<String> toRemove = new HashSet<>();
        toRemove.addAll(GOOGLE_SERVICE);
        toRemove.addAll(GOOGLE_APP);

        try {
            List<PackageInfo> installed = blackBoxCore.getInstalledPackages(0, userId);
            for (PackageInfo pi : installed) {
                if (pi == null || pi.packageName == null) {
                    continue;
                }
                String pkg = pi.packageName;
                if (VENDING_PKG.equals(pkg) || pkg.startsWith("com.google.android.")) {
                    toRemove.add(pkg);
                }
            }
        } catch (Throwable ignored) {
        }

        for (String pkg : toRemove) {
            try {
                blackBoxCore.clearPackage(pkg, userId);
            } catch (Throwable ignored) {
            }
        }

        for (String pkg : toRemove) {
            try {
                blackBoxCore.uninstallPackageAsUser(pkg, userId);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void remove(String packageName) {
        GOOGLE_SERVICE.remove(packageName);
        GOOGLE_APP.remove(packageName);
    }


    public static boolean isSupportGms() {
        try {
            BlackBoxCore.getPackageManager().getPackageInfo(GMS_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isInstalledGoogleService(int userId) {
        return BlackBoxCore.get().isInstalled(GMS_PKG, userId)
                && BlackBoxCore.get().isInstalled(GSF_PKG, userId)
                && BlackBoxCore.get().isInstalled(VENDING_PKG, userId);
    }
}
