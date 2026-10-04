package top.niunaijun.blackbox.fake.device;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

/** Single server-process store, so identity changes remain consistent across app processes. */
public final class IdentityStoreProvider extends ContentProvider {
    public static final String[] KEYS = {"manufacturer", "brand", "model", "device", "product",
            "fingerprint", "serial", "androidId", "deviceId", "bluetoothMac", "wifiMac"};
    private SharedPreferences prefs;
    @Override public boolean onCreate() {
        prefs = getContext().getSharedPreferences("SpaceIdentity", 0);
        return true;
    }
    @Override public synchronized Bundle call(String method, String arg, Bundle extras) {
        int user = Integer.parseInt(arg);
        if (user < 0) throw new IllegalArgumentException("Invalid space");
        String prefix = user + ".";
        if ("delete".equals(method)) {
            SharedPreferences.Editor edit = prefs.edit();
            for (String k : KEYS) { edit.remove(prefix + k); edit.remove(prefix + "default." + k); }
            if (!edit.commit()) throw new IllegalStateException("Could not save identity");
            return new Bundle();
        }
        if (!prefs.contains(prefix + "default.androidId")) {
            Bundle defaults = defaults();
            SharedPreferences.Editor edit = prefs.edit();
            for (String k : KEYS) edit.putString(prefix + "default." + k, defaults.getString(k));
            if (!edit.commit()) throw new IllegalStateException("Could not create identity");
        }
        if ("random".equals(method) || "reset".equals(method) || "save".equals(method)) {
            SharedPreferences.Editor edit = prefs.edit();
            if ("reset".equals(method)) {
                for (String k : KEYS) edit.remove(prefix + k);
            } else {
                Bundle values = "random".equals(method) ? random() : extras;
                if (values == null) throw new IllegalArgumentException("Missing values");
                for (String k : KEYS) if (values.containsKey(k)) edit.putString(prefix + k, values.getString(k));
            }
            if (!edit.commit()) throw new IllegalStateException("Could not save identity");
        } else if (!"get".equals(method)) throw new IllegalArgumentException("Unknown identity operation");
        Bundle result = new Bundle();
        for (String k : KEYS) result.putString(k, prefs.getString(prefix + k,
                prefs.getString(prefix + "default." + k, "")));
        return result;
    }
    private static Bundle random() {
        Bundle b = new Bundle();
        b.putString("androidId", IdentityValues.hex(8));
        b.putString("deviceId", IdentityValues.deviceId());
        b.putString("serial", IdentityValues.digits(20));
        b.putString("bluetoothMac", IdentityValues.mac());
        b.putString("wifiMac", IdentityValues.mac());
        return b;
    }
    private static Bundle defaults() {
        Bundle b = random();
        b.putString("manufacturer", Build.MANUFACTURER);
        b.putString("brand", Build.BRAND);
        b.putString("model", Build.MODEL);
        b.putString("device", Build.DEVICE);
        b.putString("product", Build.PRODUCT);
        b.putString("fingerprint", Build.FINGERPRINT);
        return b;
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
