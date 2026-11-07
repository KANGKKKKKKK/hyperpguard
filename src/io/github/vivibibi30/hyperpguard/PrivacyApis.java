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

            // 手机号
            int line = Hooks.retAllByName(m, cl, "android.telephony.TelephonyManager",
                    new String[]{"getLine1Number", "getLine1NumberForSubscriber"}, "");
            n += line;
            Logs.i("line number hooked=" + line);

            // 硬件序列号：与系统无权限时的返回值保持一致
            int serial = Hooks.retAllByName(m, cl, "android.os.Build",
                    new String[]{"getSerial"}, "unknown");
            Logs.i("serial hooked=" + serial);

            // Android ID：只拦读取，不影响其他 Settings 项
            hookAndroidId(cl);
        }

        // 以下默认关闭，会影响功能（定位、信号、Wi-Fi 列表、通讯录）
        if (Config.on(Config.K_LOC, false)) hookLocation(cl);
        if (Config.on(Config.K_CELL, false)) hookCell(cl);
        if (Config.on(Config.K_WIFI, false)) hookWifi(cl);
        if (Config.on(Config.K_RESOLVER, false)) hookResolver(cl);
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

    /** Android ID：Settings.Secure.getString(cr, "android_id") */
    private void hookAndroidId(ClassLoader cl) {
        try {
            Class<?> secure = Class.forName("android.provider.Settings$Secure", false, cl);
            int n = 0;
            for (Method method : secure.getDeclaredMethods()) {
                if (!method.getName().equals("getString")) continue;
                Class<?>[] p = method.getParameterTypes();
                if (p.length != 2 || p[1] != String.class) continue;
                m.hook(method).intercept(chain -> {
                    Object key = chain.getArg(1);
                    if ("android_id".equals(key)) return null;
                    return chain.proceed();
                });
                n++;
            }
            Logs.i("android_id hooked=" + n);
        } catch (Throwable t) {
            Logs.i("android_id miss: " + t);
        }
    }

    /** 定位：返回 null / 不注册监听 */
    private void hookLocation(ClassLoader cl) {
        try {
            Class<?> lm = Class.forName("android.location.LocationManager", false, cl);
            int n = 0;
            for (Method method : lm.getDeclaredMethods()) {
                String name = method.getName();
                if (!name.equals("getLastKnownLocation")
                        && !name.equals("requestLocationUpdates")
                        && !name.equals("getCurrentLocation")) continue;
                try {
                    m.hook(method).intercept(chain -> {
                        Class<?> rt = ((java.lang.reflect.Method) chain.getExecutable()).getReturnType();
                        return rt == void.class || rt == boolean.class ? null : null;
                    });
                    n++;
                } catch (Throwable ignored) {
                }
            }
            Logs.i("location hooked=" + n);
        } catch (Throwable t) {
            Logs.i("location miss: " + t);
        }
    }

    /** 基站信息 */
    private void hookCell(ClassLoader cl) {
        try {
            Class<?> tm = Class.forName("android.telephony.TelephonyManager", false, cl);
            int n = 0;
            for (Method method : tm.getDeclaredMethods()) {
                String name = method.getName();
                if (!name.equals("getCellLocation") && !name.equals("getAllCellInfo")
                        && !name.equals("getNeighboringCellInfo")) continue;
                m.hook(method).intercept(chain -> {
                    Class<?> rt = ((java.lang.reflect.Method) chain.getExecutable()).getReturnType();
                    return java.util.List.class.isAssignableFrom(rt)
                            ? new java.util.ArrayList<>() : null;
                });
                n++;
            }
            Logs.i("cell hooked=" + n);
        } catch (Throwable t) {
            Logs.i("cell miss: " + t);
        }
    }

    /** Wi-Fi 扫描与连接信息里的 SSID/BSSID */
    private void hookWifi(ClassLoader cl) {
        try {
            Class<?> wm = Class.forName("android.net.wifi.WifiManager", false, cl);
            int n = 0;
            for (Method method : wm.getDeclaredMethods()) {
                if (!method.getName().equals("getScanResults")) continue;
                m.hook(method).intercept(chain -> new java.util.ArrayList<>());
                n++;
            }
            // WifiInfo 的 SSID / BSSID
            Class<?> wi = Class.forName("android.net.wifi.WifiInfo", false, cl);
            for (Method method : wi.getDeclaredMethods()) {
                String name = method.getName();
                if (!name.equals("getSSID") && !name.equals("getBSSID")) continue;
                m.hook(method).intercept(chain -> name.equals("getSSID") ? "<unknown ssid>" : "02:00:00:00:00:00");
                n++;
            }
            Logs.i("wifi hooked=" + n);
        } catch (Throwable t) {
            Logs.i("wifi miss: " + t);
        }
    }

    /** 通讯录 / 通话记录 / 短信 查询：只拦这三类 uri，其余放行 */
    private void hookResolver(ClassLoader cl) {
        try {
            Class<?> cr = Class.forName("android.content.ContentResolver", false, cl);
            int n = 0;
            for (Method method : cr.getDeclaredMethods()) {
                if (!method.getName().equals("query")) continue;
                Class<?>[] p = method.getParameterTypes();
                if (p.length == 0 || p[0] != android.net.Uri.class) continue;
                m.hook(method).intercept(chain -> {
                    Object u = chain.getArg(0);
                    String s = u == null ? "" : u.toString();
                    if (s.startsWith("content://call_log")
                            || s.startsWith("content://sms")
                            || s.startsWith("content://mms")
                            || s.startsWith("content://com.android.contacts")) {
                        return null;
                    }
                    return chain.proceed();
                });
                n++;
            }
            Logs.i("resolver hooked=" + n);
        } catch (Throwable t) {
            Logs.i("resolver miss: " + t);
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
