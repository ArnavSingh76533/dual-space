package top.niunaijun.blackbox.fake.device;

import android.net.Uri;
import android.os.Bundle;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.NativeCore;

public final class DeviceSpoofManager {
    private DeviceSpoofManager() {}
    public static Bundle values(int userId) { return operation("get", userId, null); }
    private static Bundle operation(String method, int user, Bundle values) {
        Bundle result = BlackBoxCore.getContext().getContentResolver().call(
                Uri.parse("content://" + BlackBoxCore.getHostPkg() + ".space.identity"),
                method, Integer.toString(user), values);
        if (result == null) throw new IllegalStateException("Identity service unavailable");
        return result;
    }
    public static void generateRandom(int user) { operation("random", user, null); }
    public static void resetToDefaults(int user) { operation("reset", user, null); }
    public static void delete(int user) { operation("delete", user, null); }
    public static Bundle currentValues() { return values(Math.max(0, BActivityThread.getUserId())); }
    public static DeviceSpoofProfile getProfile(int user) {
        Bundle b = values(user);
        return new DeviceSpoofProfile(b.getString("manufacturer"), b.getString("brand"),
                b.getString("model"), b.getString("device"), b.getString("product"),
                b.getString("fingerprint"), b.getString("serial"), b.getString("androidId"));
    }
    public static void applyToCurrentProcess(int user) {
        DeviceSpoofProfile p = getProfile(user);
        DeviceBuildSpoofer.apply(p);
        NativeCore.setDeviceSpoof(p.manufacturer, p.brand, p.model, p.device,
                p.product, p.fingerprint, p.serial);
    }
    public static String getAndroidIdForCurrentUser() { return currentValues().getString("androidId"); }
    public static boolean isValidAndroidId(String value) { return value != null && value.matches("(?i)[0-9a-f]{16}"); }
}
