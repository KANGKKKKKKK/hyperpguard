package io.github.vivibibi30.hyperpguard;

import java.lang.reflect.Method;
import java.util.ArrayList;

import io.github.libxposed.api.XposedModule;

/** 隐私 API 层：在数据来源处截断。 */
final class PrivacyApis {

    private final XposedModule m;

    PrivacyApis(XposedModule m) {
        this.m = m;
    }

    void install(ClassLoader cl, boolean appList, boolean usage,
                 boolean ids, boolean sms, boolean xmsf) {
        if (appList) {
            Hooks.ret(m, cl, "android.app.ApplicationPackageManager",
                    "getInstalledPackages", new Class<?>[]{int.class}, new ArrayList<Object>());
            Hooks.ret(m, cl, "android.app.ApplicationPackageManager",
                    "getInstalledApplications", new Class<?>[]{int.class}, new ArrayList<Object>());
        }
        if (usage) {
            Hooks.ret(m, cl, "android.app.usage.UsageStatsManager",
                    "queryEvents", new Class<?>[]{long.class, long.class}, null);
            Hooks.ret(m, cl, "android.app.usage.UsageStatsManager",
                    "queryUsageStats", new Class<?>[]{int.class, long.class, long.class},
                    new ArrayList<Object>());
        }
        if (ids) {
            String[] aosp = {"getDeviceId", "getImei", "getMeid",
                    "getSubscriberId", "getSimSerialNumber", "getDeviceIdForSlot",
                    "getImeiForSlot", "getMeidForSlot", "getSubscriberIdForSlot",
                    "getSimSerialNumberForSlot"};
            // AOSP：连同带 slot 参数的重载一起拦
            int n = Hooks.retAllByName(m, cl, "android.telephony.TelephonyManager", aosp, null);
            // 小米私有类：registration / systemhelper 等组件走这里，AOSP 那层拦不住
            n += Hooks.retAllByName(m, cl, "miui.telephony.TelephonyManager", aosp, null);
            n += Hooks.retAllByName(m, cl, "miui.telephony.TelephonyManagerEx", aosp, null);
            Logs.i("telephony ids hooked=" + n);

            // MAC：给固定值而不是 null，避免调用方判空崩溃
            int mac = Hooks.retAllByName(m, cl, "android.net.wifi.WifiInfo",
                    new String[]{"getMacAddress"}, "02:00:00:00:00:00");
            Logs.i("mac hooked=" + mac);
        }
        if (sms) {
            Hooks.ret(m, cl, "android.telephony.SmsManager", "sendTextMessage",
                    new Class<?>[]{String.class, String.class, String.class, String.class,
                            android.app.PendingIntent.class}, null);
            Hooks.ret(m, cl, "android.telephony.SmsManager", "sendMultipartTextMessage",
                    new Class<?>[]{String.class, String.class, java.util.List.class,
                            java.util.List.class, java.util.List.class}, null);
            Hooks.ret(m, cl, "android.telephony.SmsManager", "sendDataMessage",
                    new Class<?>[]{String.class, String.class, short.class, byte[].class,
                            android.app.PendingIntent.class}, null);
        }
        if (xmsf) {
            hookXmsfBroadcast(cl, "android.content.ContextWrapper");
            hookXmsfBroadcast(cl, "android.content.ContextImpl");
        }
    }

    private void hookXmsfBroadcast(ClassLoader cl, String cls) {
        try {
            Class<?> c = Class.forName(cls, false, cl);
            Method method = c.getDeclaredMethod("sendBroadcast", android.content.Intent.class);
            m.hook(method).intercept(chain -> {
                Object a = chain.getArg(0);
                if (a instanceof android.content.Intent) {
                    String act = ((android.content.Intent) a).getAction();
                    if ("com.xiaomi.xmsf.push.XMSF_UPLOAD_ACTIVE".equals(act)) {
                        Logs.i("block XMSF upload broadcast");
                        return null;
                    }
                }
                return chain.proceed();
            });
            Logs.i("hook ok " + cls + "#sendBroadcast(XMSF 过滤)");
        } catch (Throwable t) {
            Logs.i("hook miss " + cls + "#sendBroadcast -> " + t);
        }
    }
}
