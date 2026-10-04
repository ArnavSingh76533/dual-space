package com.arnav.dualspace;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;

public final class DualSpaceApplication extends Application {
    public static volatile String engineError;
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override public String getHostPackageName() { return base.getPackageName(); }
                @Override public boolean isEnableDaemonService() {
                    return base.getSharedPreferences("DualSpace", 0).getBoolean("background", true);
                }
                @Override public String getLogSenderChatId() { return null; }
            });
        } catch (Throwable e) {
            engineError = e.toString();
            Log.e("DualSpace", "Engine attach failed", e);
        }
    }
    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        if (engineError == null) {
            try { BlackBoxCore.get().doCreate(); }
            catch (Throwable e) { engineError = e.toString(); Log.e("DualSpace", "Engine startup failed", e); }
        }
    }
}
