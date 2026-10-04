package com.arnav.nimbus;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

final class Ui {
    static final int BG = 0xff111217, CARD = 0xff323139, TEXT = 0xffe5e3ec,
            MUTED = 0xffbcbac6, BLUE = 0xff345fef, ACCENT = 0xffb2bfff;
    static int dp(Context c, float v) { return Math.round(c.getResources().getDisplayMetrics().density * v); }
    static GradientDrawable bg(int color, int radius, Context c) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius)); return d;
    }
    static TextView text(Context c, String s, int size, int color) {
        TextView v = new TextView(c); v.setText(s); v.setTextSize(size); v.setTextColor(color); return v;
    }
    static TextView button(Context c, String s, Runnable action) {
        TextView v = text(c, s, 16, ACCENT); v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        v.setMinHeight(dp(c, 52)); v.setBackground(bg(CARD, 14, c));
        v.setOnClickListener(w -> action.run()); v.setFocusable(true); return v;
    }
    static LinearLayout column(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    static LinearLayout row(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    static EditText input(Context c, String hint) {
        EditText v = new EditText(c); v.setSingleLine(true); v.setTextColor(TEXT); v.setHintTextColor(MUTED);
        v.setTextSize(16); v.setHint(hint); v.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        return v;
    }
    static void margin(View v, int top, Context c) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(c, top); v.setLayoutParams(p);
    }
    static LinearLayout card(Context c) {
        LinearLayout v = column(c); v.setBackground(bg(CARD, 14, c));
        v.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12)); margin(v, 12, c); return v;
    }
    static void bold(TextView v) { v.setTypeface(v.getTypeface(), Typeface.BOLD); }
}
