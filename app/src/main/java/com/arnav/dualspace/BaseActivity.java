package com.arnav.dualspace;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public abstract class BaseActivity extends AppCompatActivity {
    static final ExecutorService IO = Executors.newSingleThreadExecutor();
    LinearLayout root, toolbar;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        root = Ui.column(this); root.setBackgroundColor(Ui.BG);
        toolbar = Ui.row(this); toolbar.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 64)));
        setContentView(root);
    }
    void title(String title, boolean back) {
        toolbar.removeAllViews();
        if (back) icon("‹", "Back", () -> finish());
        TextView text = Ui.text(this, title, 22, Ui.TEXT);
        toolbar.addView(text, new LinearLayout.LayoutParams(0, -1, 1)); text.setGravity(Gravity.CENTER_VERTICAL);
    }
    TextView icon(String glyph, String description, Runnable action) {
        TextView v = Ui.text(this, glyph, 24, Ui.TEXT); v.setGravity(Gravity.CENTER); v.setContentDescription(description);
        v.setOnClickListener(w -> action.run()); v.setFocusable(true);
        toolbar.addView(v, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48))); return v;
    }
    void alert(String title, String message) { if (!isFinishing()) new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show(); }
    void confirm(String title, String message, Runnable action) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (d, w) -> action.run()).show();
    }
    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    <T> void background(Callable<T> work, Consumer<T> done) {
        IO.execute(() -> {
            try {
                T value = work.call();
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) done.accept(value); });
            } catch (Throwable e) {
                android.util.Log.e("DualSpace", "Operation failed", e);
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) alert("Could not complete", e.getMessage() == null ? e.toString() : e.getMessage()); });
            }
        });
    }
    <T> void busy(String message, Callable<T> work, Consumer<T> done) {
        LinearLayout body = Ui.row(this); body.setPadding(Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24));
        body.addView(new ProgressBar(this), new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));
        TextView text = Ui.text(this, message, 16, Ui.TEXT); text.setPadding(Ui.dp(this, 16), 0, 0, 0); body.addView(text);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(body).setCancelable(false).create(); dialog.show();
        IO.execute(() -> {
            T result = null; Throwable error = null;
            try { result = work.call(); } catch (Throwable e) { error = e; android.util.Log.e("DualSpace", "Operation failed", e); }
            T value = result; Throwable failure = error;
            runOnUiThread(() -> {
                if (!isDestroyed()) dialog.dismiss();
                if (isFinishing() || isDestroyed()) return;
                if (failure == null) done.accept(value);
                else alert("Could not complete", failure.getMessage() == null ? failure.toString() : failure.getMessage());
            });
        });
    }
}
