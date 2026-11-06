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
        try {
            Class<?> impl = Class.forName(
                    "com.xiaomi.security.xsof.MiSafetyDetectService$MiSafetyDetectImpl", false, cl);
            int n = 0;
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
            Logs.i("xsof aidl hooked=" + n);
        } catch (Throwable t) {
            Logs.i("xsof aidl miss: " + t);
        }
    }

    private void installDbWrites(ClassLoader cl) {
        try {
            Class<?> rk = Class.forName("Ra.k", false, cl);
            int n = 0;
            for (Method method : rk.getDeclaredMethods()) {
                if (method.getReturnType() != void.class) continue;
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
                Class<?>[] pts = method.getParameterTypes();
                if (pts.length == 0) continue;
                if (!pts[0].getName().startsWith("Qa.")) continue;
                m.hook(method).intercept(chain -> null);
                n++;
            }
            Logs.i("xsof db write hooked=" + n);
        } catch (Throwable t) {
            Logs.i("xsof db write miss: " + t);
        }
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
            Logs.i("xsof score miss: " + t);
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
