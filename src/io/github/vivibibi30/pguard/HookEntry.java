package io.github.vivibibi30.pguard;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 主入口：按包名分发，每条规则独立开关。
 *
 * 拦截三层：URL / DNS 解析 / Socket，任一层命中即断。
 * 每条规则都是「域名后缀 (+ 可选精确路径)」，不做全局路径模糊匹配，
 * 避免误伤同域下的功能接口（如 virus-engine.sec.miui.com 病毒库下载）。
 */
public class HookEntry implements IXposedHookLoadPackage {

    private static final class Rule {
        final String host;
        final String path;   // null = 不限路径；非 null = 必须以此前缀开头

        Rule(String host) {
            this(host, null);
        }

        Rule(String host, String path) {
            this.host = host;
            this.path = path;
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (lp == null || lp.packageName == null) return;
        if (!Config.masterOn(lp)) return;

        String pkg = lp.packageName;

        /* ---------------- SystemHelper：联通 DM 上报 ---------------- */
        if ("com.mobiletools.systemhelper".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_SYSHELPER, false)) return;
            if (!Config.on(lp, Config.K_SYSHELPER, true)) return;
            Config.log("attach systemhelper");
            hookSystemHelper(lp);
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("dm.wo.com.cn"));
            guard(lp, "systemhelper", r, false);
            return;
        }

        /* ---------------- guardprovider：云查杀 / AI 通话云检测 ---------------- */
        if ("com.miui.guardprovider".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_GUARD, false)) return;
            List<Rule> r = new ArrayList<>();
            if (Config.on(lp, Config.K_GUARD_APPLIST, true)) {
                // 只拦 flash（应用特征上传），不碰 virus-engine（病毒库下载）
                r.add(new Rule("flash.sec.miui.com"));
            }
            if (Config.on(lp, Config.K_GUARD_AIGC, true)) {
                r.add(new Rule("detection-aigc.engine.miui.com"));
            }
            if (Config.on(lp, Config.K_GUARD_FRAUD, true)) {
                r.add(new Rule("fraud-detection.engine.miui.com"));
            }
            if (r.isEmpty()) return;
            Config.log("attach guardprovider, rules=" + r.size());
            guard(lp, "guardprovider", r, false);
            return;
        }

        /* ---------------- trustservice（默认关） ---------------- */
        if ("com.xiaomi.trustservice".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_TRUST, false)) return;
            if (!Config.on(lp, Config.K_TRUST, false)) return;
            Config.log("attach trustservice");
            hookMrm();
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("deviceapi.micloud.xiaomi.net"));
            guard(lp, "trustservice", r, false);
            return;
        }

        /* ---------------- analytics：方法层 + 域名层双保险 ---------------- */
        if ("com.miui.analytics".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_ANALYTICS, false)) return;
            if (!Config.on(lp, Config.K_ANALYTICS, true)) return;
            hookAnalyticsMethods(lp);
            boolean strict = Config.strict(lp);
            Config.log("attach analytics, strict=" + strict);
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("miui.com"));
            r.add(new Rule("xiaomi.com"));
            r.add(new Rule("xiaomi.net"));
            guard(lp, "analytics", r, strict);
            return;
        }

        /* ---------------- 翻译 / AI 通话识别后端（默认关） ---------------- */
        if ("com.xiaomi.aiasst.vision".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_AIASST, false)) return;
            if (!Config.on(lp, Config.K_AIASST, false)) return;
            Config.log("attach aiasst.vision");
            List<Rule> r = new ArrayList<>();
            // 只断识别上传路径，保留账号与文字翻译
            r.add(new Rule("xiaomixiaoai.com", "/speech/"));
            guard(lp, "aiasst", r, false);
            return;
        }

        /* ---------------- securitycenter ---------------- */
        if ("com.miui.securitycenter".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_SECCENTER, false)) return;
            if (!Config.on(lp, Config.K_SECCENTER, true)) return;
            Config.log("attach securitycenter");
            if (Config.on(lp, Config.K_XSOF, true)) hookXsof(lp);
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("fraud-detection.engine.miui.com"));
            r.add(new Rule("flash.sec.miui.com", "/detect/"));
            guard(lp, "securitycenter", r, false);
            return;
        }

        /* ---------------- 安装器 ---------------- */
        if ("com.miui.packageinstaller".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_PKG, false)) return;
            List<Rule> r = new ArrayList<>();
            boolean pathOn = Config.on(lp, Config.K_PATH, true);
            if (Config.on(lp, Config.K_PKG_AVL, true)) {
                r.add(new Rule("api.installer.xiaomi.com", "/avl/upload"));
            }
            if (pathOn && Config.on(lp, Config.K_PKG_STAT, true)) {
                r.add(new Rule("api.installer.xiaomi.com", "/statistics/appactive"));
            }
            // /info/layout 返回 AdModel，纯广告配置
            if (pathOn && Config.on(lp, Config.K_AD_PKG, true)) {
                r.add(new Rule("api.installer.xiaomi.com", "/info/layout"));
            }
            if (r.isEmpty()) return;
            Config.log("attach packageinstaller, rules=" + r.size());
            guard(lp, "packageinstaller", r, false);
            return;
        }

        /* ---------------- 应用商店广告 ---------------- */
        if ("com.xiaomi.market".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_AD_MARKET, false)) return;
            if (!Config.on(lp, Config.K_AD_MARKET, true)) return;
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("sdkconfig.ad.xiaomi.com"));
            Config.log("attach market(ad)");
            guard(lp, "market", r, false);
            return;
        }

        /* ---------------- 小米服务框架广告 ---------------- */
        if ("com.xiaomi.xmsf".equals(pkg)) {
            if (!Config.on(lp, Config.K_G_AD_XMSF, false)) return;
            if (!Config.on(lp, Config.K_AD_XMSF, true)) return;
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("adv.sec.miui.com"));
            Config.log("attach xmsf(ad)");
            guard(lp, "xmsf", r, false);
        }
    }

    /* ---------------- SystemHelper ---------------- */

    private static void hookSystemHelper(XC_LoadPackage.LoadPackageParam lp) {
        try {
            XposedHelpers.findAndHookMethod("c.d.a.b.f", lp.classLoader, "a",
                    String.class, byte[].class, byte[].class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            Config.log("systemhelper http dropped");
                            p.setResult(Boolean.FALSE);
                        }
                    });
        } catch (Throwable t) {
            Config.log("systemhelper http hook miss: " + t);
        }

        try {
            Class<?> db = XposedHelpers.findClass("c.d.a.b.e", lp.classLoader);
            XposedBridge.hookAllMethods(db, "a", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args != null && p.args.length == 1 && p.args[0] instanceof String) {
                        p.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            Config.log("systemhelper db hook miss: " + t);
        }
    }

    /* ---------------- analytics：数据源方法层（抗域名更换） ---------------- */

    private static void hookAnalyticsMethods(XC_LoadPackage.LoadPackageParam lp) {
        ClassLoader cl = lp.classLoader;
        // 两个上报入口：返回 null 即不产出数据
        hookMethodSafe(cl, "com.miui.analytics.onetrack.p.u", "w",
                new Class<?>[]{long.class, long.class}, null);
        hookMethodSafe(cl, "com.miui.analytics.c.f.k", "y",
                new Class<?>[]{long.class, long.class}, null);
        // 三个数据源：0 / "NONE" / false
        hookMethodSafe(cl, "com.miui.analytics.onetrack.q.c", "a",
                new Class<?>[]{android.content.Context.class}, Integer.valueOf(0));
        hookMethodSafe(cl, "com.miui.analytics.onetrack.q.c", "b",
                new Class<?>[]{android.content.Context.class}, "NONE");
        hookMethodSafe(cl, "com.miui.analytics.onetrack.q.c", "c",
                new Class<?>[0], Boolean.FALSE);
    }

    /**
     * 按类名+方法名+参数签名 hook，返回值按方法实际返回类型兜底，
     * 避免把 long/boolean 方法塞 null 导致调用方崩溃。
     */
    private static void hookMethodSafe(ClassLoader cl, final String cls, final String name,
                                       Class<?>[] types, final Object want) {
        try {
            Class<?> c = XposedHelpers.findClass(cls, cl);
            java.lang.reflect.Method m = c.getDeclaredMethod(name, types);
            m.setAccessible(true);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    Class<?> rt = ((java.lang.reflect.Method) p.method).getReturnType();
                    if (rt == void.class) {
                        p.setResult(null);
                    } else if (rt == boolean.class) {
                        p.setResult(Boolean.FALSE);
                    } else if (rt == long.class) {
                        p.setResult(Long.valueOf(0L));
                    } else if (rt == int.class) {
                        p.setResult(Integer.valueOf(0));
                    } else if (rt == short.class) {
                        p.setResult(Short.valueOf((short) 0));
                    } else if (rt == byte.class) {
                        p.setResult(Byte.valueOf((byte) 0));
                    } else if (want != null && rt.isInstance(want)) {
                        p.setResult(want);
                    } else {
                        p.setResult(null);
                    }
                    Config.log("analytics src nulled: " + name);
                }
            });
            Config.log("hook ok " + cls + "#" + name);
        } catch (Throwable t) {
            // 类名/签名随版本变化时静默降级，域名层仍在
            Config.log("hook miss " + cls + "#" + name + " -> " + t);
        }
    }

    /* ---------------- XSOF：对外查询与写入拦截 ---------------- */
    /*
     * 目标：
     *   com.xiaomi.security.xsof.XSOFProvider  query / call / insert
     *   Pa.b (FraudDetectionManager) 的评分方法 j(...)
     * 目的：让第三方应用查不到「最近是否截图 / 通话 / 屏幕共享」等行为特征。
     */

    private static final java.util.concurrent.atomic.AtomicBoolean XSOF_HIT =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static void hookXsof(XC_LoadPackage.LoadPackageParam lp) {
        ClassLoader cl = lp.classLoader;

        try {
            Class<?> prov = XposedHelpers.findClass("com.xiaomi.security.xsof.XSOFProvider", cl);

            // 查询：返回空 Cursor（不是 null），兼容不判空的调用方
            XposedBridge.hookAllMethods(prov, "query", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (XSOF_HIT.compareAndSet(false, true)) Config.log("xsof query blocked (first)");
                    p.setResult(new android.database.MatrixCursor(new String[]{"_id"}));
                }
            });
            // call：返回空 Bundle
            XposedBridge.hookAllMethods(prov, "call", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    p.setResult(new android.os.Bundle());
                }
            });
            // 写入：null 是 ContentProvider 表示“未插入”的标准返回
            XposedBridge.hookAllMethods(prov, "insert", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    p.setResult(null);
                }
            });
            Config.log("xsof provider hooked");
        } catch (Throwable t) {
            Config.log("xsof provider miss: " + t);
        }

        // AIDL 层：IMiSafetyDetectServer 的 7 个查询方法，统一回 1017
        try {
            Class<?> impl = XposedHelpers.findClass(
                    "com.xiaomi.security.xsof.MiSafetyDetectService$MiSafetyDetectImpl", cl);
            int n = 0;
            for (final java.lang.reflect.Method m : impl.getDeclaredMethods()) {
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length < 2) continue;
                if (!pts[pts.length - 1].getName().contains("IMiSafetyDetectCallback")) continue;
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        Object cb = p.args[p.args.length - 1];
                        if (cb != null) {
                            try {
                                java.lang.reflect.Method g2 = cb.getClass()
                                        .getMethod("G2", String.class, int.class);
                                g2.invoke(cb, null, Integer.valueOf(1017));
                            } catch (Throwable ignored) {
                            }
                        }
                        p.setResult(null);
                    }
                });
                n++;
            }
            Config.log("xsof aidl hooked=" + n);
        } catch (Throwable t) {
            Config.log("xsof aidl miss: " + t);
        }

        // 本地写入：Ra.k（SQLiteOpenHelper）的写入方法，直接返回不落库
        try {
            Class<?> rk = XposedHelpers.findClass("Ra.k", cl);
            int n = 0;
            for (final java.lang.reflect.Method m : rk.getDeclaredMethods()) {
                if (m.getReturnType() != void.class) continue;
                if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length == 0) continue;
                if (!pts[0].getName().startsWith("Qa.")) continue;
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        p.setResult(null);
                    }
                });
                n++;
            }
            Config.log("xsof db write hooked=" + n);
        } catch (Throwable t) {
            Config.log("xsof db write miss: " + t);
        }

        // 风险评分归零：Pa.b 的 3 参数 float 方法
        try {
            Class<?> fd = XposedHelpers.findClass("Pa.b", cl);
            int n = 0;
            for (java.lang.reflect.Method m : fd.getDeclaredMethods()) {
                if (m.getParameterCount() != 3) continue;
                if (m.getReturnType() != float.class) continue;
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        p.setResult(Float.valueOf(0f));
                    }
                });
                n++;
            }
            Config.log("xsof score hooked=" + n);
        } catch (Throwable t) {
            Config.log("xsof score miss: " + t);
        }
    }

    /* ---------------- trustservice：MRM ---------------- */

    private static void hookMrm() {
        try {
            Class<?> sm = XposedHelpers.findClass("android.os.ServiceManager", null);
            XC_MethodHook h = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    Object a0 = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                    if (a0 instanceof String && ((String) a0).toLowerCase().contains("mrm")) {
                        Config.log("mrm service blocked: " + a0);
                        p.setResult(null);
                    }
                }
            };
            XposedBridge.hookAllMethods(sm, "waitForService", h);
            XposedBridge.hookAllMethods(sm, "getService", h);
        } catch (Throwable t) {
            Config.log("mrm hook miss: " + t);
        }
    }

    /* ---------------- 三层守卫 ---------------- */

    private static void guard(final XC_LoadPackage.LoadPackageParam lp, final String tag,
                              final List<Rule> rules, final boolean strictAll) {
        if (rules.isEmpty() && !strictAll) return;

        final boolean pathOn = Config.on(lp, Config.K_PATH, true);
        final boolean sniffOn = Config.on(lp, Config.K_SNIFF, false);

        final XC_MethodHook urlHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    if (!(p.thisObject instanceof URL)) return;
                    URL u = (URL) p.thisObject;
                    String host = u.getHost();
                    if (host == null) return;
                    if (check(tag, host, u.getPath(), rules, strictAll, pathOn, sniffOn)) {
                        p.setThrowable(new IOException("blocked by PrivacyGuard: " + host));
                    }
                } catch (Throwable ignored) {
                }
            }
        };

        final XC_MethodHook dnsHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    if (p.args == null || p.args.length == 0 || !(p.args[0] instanceof String)) return;
                    String host = (String) p.args[0];
                    if (host == null) return;
                    if (check(tag, host, null, rules, strictAll, pathOn, false)) {
                        p.setThrowable(new UnknownHostException("blocked by PrivacyGuard: " + host));
                    }
                } catch (Throwable ignored) {
                }
            }
        };

        final XC_MethodHook sockHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    String host = null;
                    if (p.args != null) {
                        for (Object a : p.args) {
                            if (a instanceof InetSocketAddress) {
                                host = ((InetSocketAddress) a).getHostString();
                                break;
                            }
                            if (a instanceof String) {
                                host = (String) a;
                                break;
                            }
                            if (a instanceof InetAddress) {
                                host = ((InetAddress) a).getHostAddress();
                                break;
                            }
                        }
                    }
                    if (host == null) return;
                    if (check(tag, host, null, rules, strictAll, pathOn, false)) {
                        p.setThrowable(new IOException("blocked by PrivacyGuard: " + host));
                    }
                } catch (Throwable ignored) {
                }
            }
        };

        try {
            XposedBridge.hookAllMethods(URL.class, "openConnection", urlHook);
        } catch (Throwable t) {
            Config.log(tag + " url hook miss: " + t);
        }
        try {
            XposedBridge.hookAllMethods(InetAddress.class, "getByName", dnsHook);
            XposedBridge.hookAllMethods(InetAddress.class, "getAllByName", dnsHook);
        } catch (Throwable t) {
            Config.log(tag + " dns hook miss: " + t);
        }
        try {
            XposedBridge.hookAllConstructors(java.net.Socket.class, sockHook);
            XposedHelpers.findAndHookMethod(java.net.Socket.class, "connect",
                    SocketAddress.class, sockHook);
            XposedHelpers.findAndHookMethod(java.net.Socket.class, "connect",
                    SocketAddress.class, int.class, sockHook);
        } catch (Throwable t) {
            Config.log(tag + " socket hook miss: " + t);
        }
    }

    private static boolean check(String tag, String host, String path,
                                 List<Rule> rules, boolean strictAll,
                                 boolean pathOn, boolean sniffOn) {
        String h = host.toLowerCase();

        if (strictAll) {
            if (!isLocal(h)) {
                Config.log(tag + " X(strict) " + h);
                return true;
            }
            return false;
        }

        for (Rule r : rules) {
            if (!hostMatch(h, r.host)) continue;
            if (r.path == null) {
                Config.log(tag + " X(host) " + h);
                return true;
            }
            if (pathOn && path != null && path.startsWith(r.path)) {
                Config.log(tag + " X(host+path) " + h + path);
                return true;
            }
        }

        if (sniffOn) sniff(tag, h, path);
        return false;
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Long> SEEN =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static void sniff(String tag, String host, String path) {
        String key = tag + "|" + host;
        long now = System.currentTimeMillis();
        Long last = SEEN.get(key);
        if (last != null && now - last < 60000L) return;
        SEEN.put(key, now);
        Config.log("? " + tag + " " + host + (path == null ? "" : path));
    }

    private static boolean hostMatch(String h, String suffix) {
        String s = suffix.toLowerCase();
        return h.equals(s) || h.endsWith("." + s);
    }

    private static boolean isLocal(String h) {
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1")
                || h.startsWith("127.") || h.startsWith("192.168.")
                || h.startsWith("10.") || h.startsWith("172.16.") || h.endsWith(".local");
    }
}
