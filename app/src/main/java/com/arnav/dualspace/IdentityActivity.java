package com.arnav.dualspace;

import android.os.Bundle;
import android.widget.*;
import top.niunaijun.blackbox.fake.device.DeviceSpoofManager;

public final class IdentityActivity extends BaseActivity {
    private int space;
    private LinearLayout content;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); space = getIntent().getIntExtra("space", 0); title("Device identity", true);
        ScrollView scroll = new ScrollView(this); content = Ui.column(this);
        content.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24)); scroll.addView(content); root.addView(scroll);
        refresh();
    }
    private void refresh() {
        background(() -> DeviceSpoofManager.values(space), values -> {
            content.removeAllViews(); LinearLayout card = Ui.card(this); content.addView(card);
            field(card, "Target", SpaceRepository.name(this, space)); field(card, "Scope", "Container space");
            TextView warning = Ui.text(this, "Changing this identity stops all apps in this space. New identifiers apply when they restart. Your phone's physical identifiers stay unchanged.", 14, Ui.MUTED);
            Ui.margin(warning, 14, this); card.addView(warning);
            field(card, "Device ID", values.getString("deviceId"));
            field(card, "Android ID", values.getString("androidId"));
            field(card, "Bluetooth MAC", values.getString("bluetoothMac"));
            field(card, "Wi-Fi MAC", values.getString("wifiMac"));
            field(card, "Serial", values.getString("serial"));
            LinearLayout actions = Ui.card(this); content.addView(actions);
            actions.addView(Ui.button(this, "Generate random identity", () -> confirm("Generate a new identity?", "All apps in " + SpaceRepository.name(this, space) + " will stop. Some apps may require you to sign in again.", () -> change(true))));
            actions.addView(Ui.button(this, "Restore default identity", () -> confirm("Restore default identity?", "Restore the identity created for this space. All apps in this space will stop.", () -> change(false))));
            TextView scope = Ui.text(this, "Android API hooks provide these values to compatible apps. Bluetooth API availability varies by Android version. Apps reading native network interfaces, hardware attestation or protected identifiers may see different values.", 12, Ui.MUTED);
            Ui.margin(scope, 18, this); content.addView(scope);
        });
    }
    private void field(LinearLayout card, String label, String value) {
        LinearLayout row = Ui.row(this); Ui.margin(row, 20, this);
        TextView left = Ui.text(this, label, 14, Ui.TEXT); Ui.bold(left); row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        TextView right = Ui.text(this, value, 13, Ui.MUTED); right.setTextIsSelectable(true); right.setGravity(android.view.Gravity.END);
        row.addView(right, new LinearLayout.LayoutParams(0, -2, 1.4f)); card.addView(row);
    }
    private void change(boolean random) {
        busy("Updating identity…", () -> { SpaceRepository.resetIdentity(space, random); return true; }, ok -> { toast("Identity updated"); refresh(); });
    }
}
