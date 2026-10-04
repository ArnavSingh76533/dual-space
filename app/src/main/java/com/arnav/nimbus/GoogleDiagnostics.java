package com.arnav.nimbus;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.frameworks.BPackageManager;
import top.niunaijun.blackbox.utils.FailureMessage;

/** Local troubleshooting only; never reads Google accounts, tokens or device identifiers. */
final class GoogleDiagnostics {
    private static final String[] PACKAGES = {"com.google.android.gsf", "com.google.android.gms", "com.android.vending"};
    private GoogleDiagnostics() { }

    static String status(int space) {
        StringBuilder result = new StringBuilder();
        for (String pkg : PACKAGES) result.append(pkg).append("\n").append(virtualStatus(pkg, space)).append("\n\n");
        return result.toString();
    }

    private static String virtualStatus(String pkg, int space) {
        try {
            return BPackageManager.get().isInstalledInSpace(pkg, space) ? "Installed in this space" : "Missing from this space";
        } catch (Exception failure) {
            return "Status unavailable: " + FailureMessage.describe(failure);
        }
    }

    static String report(Context context, int space) {
        StringBuilder report = new StringBuilder("Nimbus Google setup report\n");
        report.append("App: ").append(BuildConfig.VERSION_NAME).append(" (code ").append(BuildConfig.VERSION_CODE).append(")\n");
        report.append("Android: ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append("\n");
        report.append("Device ABIs: ").append(String.join(", ", Build.SUPPORTED_ABIS)).append("\n");
        report.append("Engine: ").append(BlackBoxCore.is64Bit() ? "64-bit" : "32-bit").append("\nSpace ID: ").append(space).append("\n");
        report.append("Last setup error: ").append(SpaceRepository.prefs(context).getString("googleError." + space, "None recorded")).append("\n\n");
        PackageManager pm = context.getPackageManager();
        for (String pkg : PACKAGES) {
            report.append(pkg).append("\n").append(virtualStatus(pkg, space)).append("\n");
            try {
                PackageInfo info = pm.getPackageInfo(pkg, 0);
                ApplicationInfo app = info.applicationInfo;
                report.append("Phone version: ").append(info.versionName).append(" (code ")
                        .append(Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode).append(")\n");
                if (app != null) {
                    report.append("Phone enabled: ").append(app.enabled).append("\nBase APK readable: ")
                            .append(app.sourceDir != null && new File(app.sourceDir).canRead()).append("\n");
                    int splits = app.splitSourceDirs == null ? 0 : app.splitSourceDirs.length;
                    int readable = 0;
                    if (app.splitSourceDirs != null) for (String path : app.splitSourceDirs) {
                        if (path != null && new File(path).canRead()) readable++;
                    }
                    report.append("Split APKs readable: ").append(readable).append('/').append(splits).append("\n");
                }
            } catch (PackageManager.NameNotFoundException missing) {
                report.append("Not installed on the phone\n");
            } catch (Exception failure) {
                report.append("Phone metadata unavailable: ").append(FailureMessage.describe(failure)).append("\n");
            }
            report.append('\n');
        }
        return report.toString();
    }
}
