package top.niunaijun.blackbox.fake.service;

import android.bluetooth.BluetoothAdapter;
import android.os.IBinder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.device.DeviceSpoofManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public final class IBluetoothManagerProxy extends BinderInvocationStub {
    public IBluetoothManagerProxy() { super(BRServiceManager.get().getService("bluetooth_manager")); }
    @Override protected Object getWho() {
        try {
            return Class.forName("android.bluetooth.IBluetoothManager$Stub")
                    .getMethod("asInterface", IBinder.class)
                    .invoke(null, BRServiceManager.get().getService("bluetooth_manager"));
        } catch (ReflectiveOperationException e) { return null; }
    }
    @Override protected void inject(Object base, Object proxy) {
        replaceSystemService("bluetooth_manager");
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                Field f = BluetoothAdapter.class.getDeclaredField("mManagerService");
                f.setAccessible(true);
                f.set(adapter, proxy);
            }
        } catch (ReflectiveOperationException ignored) { }
    }
    @Override public boolean isBadEnv() { return false; }
    @ProxyMethod("getAddress")
    public static class GetAddress extends MethodHook {
        @Override protected Object hook(Object who, Method method, Object[] args) {
            return DeviceSpoofManager.currentValues().getString("bluetoothMac");
        }
    }
}
