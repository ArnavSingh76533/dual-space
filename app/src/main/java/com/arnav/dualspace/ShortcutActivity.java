package com.arnav.dualspace;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import top.niunaijun.blackbox.BlackBoxCore;

public final class ShortcutActivity extends Activity {
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String pkg = getIntent().getStringExtra("package"); int space = getIntent().getIntExtra("space", -1);
        if (space < 0 || pkg == null || !pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")) { finish(); return; }
        BaseActivity.IO.execute(() -> {
            boolean opened = false;
            try { if (BlackBoxCore.get().isInstalled(pkg, space)) opened = BlackBoxCore.get().launchApk(pkg, space); } catch (Throwable ignored) { }
            boolean ok = opened;
            runOnUiThread(() -> { if (!ok) Toast.makeText(this, "This shortcut's app or space is unavailable", Toast.LENGTH_LONG).show(); finish(); });
        });
    }
}
