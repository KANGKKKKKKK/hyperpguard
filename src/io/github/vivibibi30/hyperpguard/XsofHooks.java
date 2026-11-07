package io.github.vivibibi30.hyperpguard;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;

/** XSOF 四层：Provider / AIDL / 写库 / 评分，以及 MRM 服务屏蔽。 */
final class XsofHooks {

    private static final AtomicBoolean XSOF_HIT = new AtomicBoolean(false);

    private final XposedModule m;

    XsofHooks(XposedModule m) {
        this.m = m;
    }

    void install(ClassLoader cl) {
        installProvider(cl);
        installAidl(cl);
        installDbWrites(cl);
        installScore(cl);
    }

    private void installProvider(ClassLoader cl) {
        try {
            Class<?> prov = Class.forName("com.xiaomi.security.xsof.XSOFProvider", false, cl);
            Hooks.allByName(m, prov, "query", chain -> {
                if (XSOF_HIT.compareAndSet(false, true)) Logs.i("xsof query blocked (first)");
                return new android.database.MatrixCursor(new String[]{"_id"});
            });
            Hooks.allByName(m, prov, "call", chain -> new Bundle());
            Hooks.allByName(m, prov, "insert", chain -> null);
            Logs.i("xsof provider hooked");
        } catch (Throwable t) {
            Logs.i("xsof provider miss: " + t);
        }
    }

    private void installAidl(ClassLoader cl) {
        int n = hookAidl(cl, "com.xiaomi.security.xsof.MiSafetyDetectService$MiSafetyDetectImpl");
        if (n == 0) {
            java.util.List<String> cands = Hooks.classNames(cl, s -> s.contains("MiSafetyDetect"), 50);
            for (String c : cands) n += hookAidl(cl, c);
            Logs.i("xsof aidl fallback cands=" + cands.size());
        }
        Logs.i("xsof aidl hooked=" + n);
    }

    private int hookAidl(ClassLoader cl, String className) {
        int n = 0;
        try {
            Class<?> impl = Class.forName(className, false, cl);
            for (Method method : impl.getDeclaredMethods()) {
                Class<?>[] pts = method.getParameterTypes();
                if (pts.length < 2) continue;
                if (!pts[pts.length - 1].getName().contains("IMiSafetyDetectCallback")) continue;
                m.hook(method).intercept(chain -> {
                    Object cb = chain.getArg(chain.getArgs().size() - 1);
                    if (cb != null) {
                        try {
                            Method g2 = cb.getClass().getMethod("G2", String.class, int.class);
                            g2.invoke(cb, null, Integer.valueOf(1017));
                        } catch (Throwable ignored) {
                        }
                    }
                    return null;
                });
                n++;
            }
        } catch (Throwable ignored) {
        }
        return n;
    }

    private void installDbWrites(ClassLoader cl) {
        int n = hookDbWritesIn(cl, "Ra.k");
        if (n == 0) {
            n = Hooks.hookBySignature(m,
                    Hooks.classNames(cl, XsofHooks::obfuscatedLikely, 8000), cl,
                    XsofHooks::isDbWrite, chain -> null);
            Logs.i("xsof db write fallback=" + n);
        }
        Logs.i("xsof db write hooked=" + n);
    }

    private int hookDbWritesIn(ClassLoader cl, String className) {
        int n = 0;
        try {
            Class<?> c = Class.forName(className, false, cl);
            for (Method method : c.getDeclaredMethods()) {
                if (!isDbWrite(method)) continue;
                try {
                    m.hook(method).intercept(chain -> null);
                    n++;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return n;
    }

    /** 版本变化后混淆类名会变，靠"短名或 XSOF 包内"缩小扫描范围 */
    private static boolean obfuscatedLikely(String name) {
        return name.startsWith("com.xiaomi.security.xsof") || name.indexOf('.') < 0 || name.length() <= 5;
    }

    /** 写行为库的出口：void + public + 首参是 Qa.* 或 ContentValues */
    private static boolean isDbWrite(Method method) {
        if (method.getReturnType() != void.class) return false;
        if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) return false;
        Class<?>[] pts = method.getParameterTypes();
        if (pts.length == 0) return false;
        String p0 = pts[0].getName();
        return p0.startsWith("Qa.") || p0.equals("android.content.ContentValues");
    }

    private void installScore(ClassLoader cl) {
        try {
            Class<?> fd = Class.forName("Pa.b", false, cl);
            int n = 0;
            for (Method method : fd.getDeclaredMethods()) {
                if (method.getParameterCount() != 3) continue;
                if (method.getReturnType() != float.class) continue;
                m.hook(method).intercept(chain -> Float.valueOf(0f));
                n++;
            }
            Logs.i("xsof score hooked=" + n);
        } catch (Throwable t) {
            Logs.i("xsof score miss (Pa.b 名随版本变化，未做宽泛扫描): " + t);
        }
    }

    void mrm(ClassLoader cl) {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager", false,
                    ClassLoader.getSystemClassLoader());
            for (Method method : sm.getDeclaredMethods()) {
                if (!method.getName().equals("getService")
                        && !method.getName().equals("waitForService")) continue;
                if (method.getParameterCount() != 1) continue;
                m.hook(method).intercept(chain -> {
                    Object a0 = chain.getArg(0);
                    if (a0 instanceof String && ((String) a0).toLowerCase().contains("mrm")) {
                        return null;
                    }
                    return chain.proceed();
                });
            }
            Logs.i("mrm hooked");
        } catch (Throwable t) {
            Logs.i("mrm hook miss: " + t);
        }
    }
}
