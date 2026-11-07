package io.github.vivibibi30.hyperpguard;

import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * 模块入口（libxposed 新 API）。
 *
 * 拦截分四层，各自独立成文件：
 *   NetGuard      网络出口（URL / InetAddress / Socket）
 *   PrivacyApis   数据来源（应用列表 / 使用记录 / 设备标识 / 短信 / XMSF 广播）
 *   AnalyticsHooks 统计方法层（固定目标 + 签名扫描）
 *   XsofHooks     XSOF Provider / AIDL / 写库 / 评分 + MRM
 */
public class HookEntry extends XposedModule {

    private final NetGuard net = new NetGuard(this);
    private final PrivacyApis privacy = new PrivacyApis(this);
    private final AnalyticsHooks analytics = new AnalyticsHooks(this);
    private final XsofHooks xsof = new XsofHooks(this);

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        Logs.i("module loaded: " + param.getProcessName());
    }

    private final java.util.concurrent.atomic.AtomicBoolean dispatched =
            new java.util.concurrent.atomic.AtomicBoolean();

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        final String pkg = param.getPackageName();
        final ClassLoader cl = param.getClassLoader();
        if (pkg == null || cl == null) return;

        android.app.Application app = currentApplication();
        if (app != null) {
            // Application 已存在：拿到本进程 Context，配置和 hook 都按真实开关走
            Config.load(app.getApplicationContext());
            dispatchOnce(pkg, cl);
            return;
        }
        // 子进程常见情况：onPackageReady 时 Application 还没创建
        if (hookApplicationOnCreate(pkg, cl)) {
            return;
        }
        Config.load(activityThreadContext());
        dispatchOnce(pkg, cl);
    }

    /** 等 Application.onCreate 再读配置并装 hook，返回是否挂上 */
    private boolean hookApplicationOnCreate(final String pkg, final ClassLoader cl) {
        try {
            java.lang.reflect.Method m = android.app.Application.class.getDeclaredMethod("onCreate");
            hook(m).intercept(chain -> {
                Object self = chain.getThisObject();
                if (self instanceof android.app.Application) {
                    Config.load(((android.app.Application) self).getApplicationContext());
                }
                dispatchOnce(pkg, cl);
                return chain.proceed();
            });
            // 兜底：Application 迟迟不创建时，用默认值先把拦截装上
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
                if (!dispatched.get()) {
                    Logs.i("onCreate timeout, use defaults: " + pkg);
                    Config.load(activityThreadContext());
                    dispatchOnce(pkg, cl);
                }
            }, "hyperpguard-fallback").start();
            Logs.i("waiting Application.onCreate: " + pkg);
            return true;
        } catch (Throwable t) {
            Logs.i("onCreate hook fail: " + t);
            return false;
        }
    }

    private void dispatchOnce(String pkg, ClassLoader cl) {
        if (!dispatched.compareAndSet(false, true)) return;
        if (!Config.masterOn()) {
            Logs.i("master off, skip: " + pkg);
            return;
        }
        try {
            dispatch(pkg, cl);
        } catch (Throwable t) {
            Logs.i("dispatch fail " + pkg + ": " + t);
        }
    }

    /** 进程已创建 Application 时直接取；早期返回 null */
    private android.app.Application currentApplication() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            if (thread == null) return null;
            java.lang.reflect.Field f = at.getDeclaredField("mInitialApplication");
            f.setAccessible(true);
            Object app = f.get(thread);
            return app instanceof android.app.Application ? (android.app.Application) app : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 系统 Context 的包名是 android，普通应用拿它查 provider 会被 AMS 拒。
     * LoadedApk#makeApplication 返回时 Application 已 attach，可用它自己的 Context 再读一次。
     */
    private void hookMakeApplication(final String pkg, final ClassLoader cl) {
        try {
            Class<?> loadedApk = Class.forName("android.app.LoadedApk", false, cl);
            for (java.lang.reflect.Method m : loadedApk.getDeclaredMethods()) {
                if (!m.getName().equals("makeApplication")) continue;
                if (m.getReturnType() != android.app.Application.class) continue;
                hook(m).intercept(chain -> {
                    Object r = chain.proceed();
                    try {
                        android.app.Application app = (android.app.Application) r;
                        Config.load(app.getApplicationContext());
                        Logs.i("app ctx config: " + Config.size());
                    } catch (Throwable t) {
                        Logs.i("app ctx config fail: " + t);
                    }
                    return r;
                });
                Logs.i("makeApplication hooked: " + pkg);
                return;
            }
            Logs.i("makeApplication not found: " + pkg);
        } catch (Throwable t) {
            Logs.i("makeApplication hook fail: " + t);
        }
    }

    /** 目标进程早期的系统 Context，只用来查询开关；拿不到就按默认值走 */
    private android.content.Context activityThreadContext() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            if (thread == null) return null;
            Object ctx = at.getMethod("getSystemContext").invoke(thread);
            return ctx instanceof android.content.Context ? (android.content.Context) ctx : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private void dispatch(String pkg, ClassLoader cl) {
        if ("com.mobiletools.systemhelper".equals(pkg)) {
            if (!Config.on(Config.K_G_SYSHELPER, true)) return;
            Logs.i("attach systemhelper");
            privacy.install(cl, pkg, false, false, true, false, false);
            boolean upload = Hooks.ret(this, cl, "c.d.a.b.f", "a",
                    new Class<?>[]{String.class, byte[].class, byte[].class}, Boolean.FALSE);
            boolean write = Hooks.ret(this, cl, "c.d.a.b.e", "a", new Class<?>[]{String.class}, null);
            if (!upload) {
                // 类名随版本变化时，按签名找 (String, byte[], byte[]) 的数据出口
                int n = Hooks.hookBySignature(this,
                        Hooks.classNames(cl, s -> s.indexOf('.') < 0 || s.length() <= 6
                                || s.startsWith("com.mobiletools"), 8000), cl,
                        m -> {
                            Class<?>[] p = m.getParameterTypes();
                            return p.length == 3 && p[0] == String.class
                                    && p[1] == byte[].class && p[2] == byte[].class;
                        },
                        chain -> {
                            java.lang.reflect.Executable ex = chain.getExecutable();
                            return ex instanceof java.lang.reflect.Method
                                    && ((java.lang.reflect.Method) ex).getReturnType() == boolean.class
                                    ? Boolean.FALSE : null;
                        });
                Logs.i("systemhelper upload fallback=" + n);
            }
            Logs.i("systemhelper direct upload=" + upload + " write=" + write);
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_SYSHELPER, true)) r.add(new NetGuard.Rule("dm.wo.com.cn"));
            net.install(cl, "systemhelper", r, Config.on(Config.K_SYSHELPER_ALLNET, true));
            return;
        }

        if ("com.miui.guardprovider".equals(pkg)) {
            if (!Config.on(Config.K_G_GUARD, true)) return;
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_GUARD_APPLIST, true)) r.add(new NetGuard.Rule("flash.sec.miui.com"));
            if (Config.on(Config.K_GUARD_AIGC, true)) r.add(new NetGuard.Rule("detection-aigc.engine.miui.com"));
            if (Config.on(Config.K_GUARD_FRAUD, true)) r.add(new NetGuard.Rule("fraud-detection.engine.miui.com"));
            if (Config.on(Config.K_GP_CLOUDSCAN, false)) r.add(new NetGuard.Rule("virus-engine.sec.miui.com"));
            Logs.i("attach guardprovider, rules=" + r.size());
            privacy.install(cl, pkg, true, false, true, false, false);
            if (Config.on(Config.K_GP_SCREENGUARD, true)) {
                CaptureHooks.screenCapture(this, cl);
            }
            if (!r.isEmpty()) net.install(cl, "guardprovider", r, false);
            return;
        }

        if ("com.miui.dmregservice".equals(pkg)) {
            // 移动 DM 组件：App 清单、SIM 标识、基站、Wi-Fi/蓝牙扫描、BSSID
            if (!Config.on(Config.K_G_DMREG, true)) return;
            Logs.i("attach dmregservice");
            privacy.install(cl, pkg, true, false, true, false, false);
            if (Config.on(Config.K_DMREG, true)) {
                List<NetGuard.Rule> r = new ArrayList<>();
                r.add(new NetGuard.Rule("fxltsbl.com"));
                net.install(cl, "dmregservice", r, false);
            }
            return;
        }

        if ("com.xiaomi.trustservice".equals(pkg)) {
            if (!Config.on(Config.K_G_TRUST, true)) return;
            Logs.i("attach trustservice");
            if (Config.on(Config.K_TRUST, false)) xsof.mrm(cl);
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("deviceapi.micloud.xiaomi.net"));
            if (Config.on(Config.K_TRUST_FIND, false)) {
                r.add(new NetGuard.Rule("find.api.micloud.xiaomi.net"));
            }
            net.install(cl, "trustservice", r, false);
            return;
        }

        if ("com.miui.analytics".equals(pkg)) {
            if (!Config.on(Config.K_G_ANALYTICS, true)) return;
            if (!Config.on(Config.K_ANALYTICS, true)) return;
            Logs.i("attach analytics");
            analytics.install(cl);
            privacy.install(cl, pkg, true, true, true, false, true);
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("miui.com"));
            r.add(new NetGuard.Rule("xiaomi.com"));
            r.add(new NetGuard.Rule("xiaomi.net"));
            r.add(new NetGuard.Rule("onetrack.mi.com"));
            r.add(new NetGuard.Rule("pubsub.googleapis.com"));
            if (Config.on(Config.K_ANALYTICS_DOH, true)) {
                r.add(new NetGuard.Rule("dns.google"));
                r.add(new NetGuard.Rule("8.8.8.8"));
                r.add(new NetGuard.Rule("8.8.4.4"));
            }
            net.install(cl, "analytics", r, Config.on(Config.K_ANALYTICS_ALLNET, true));
            return;
        }

        if ("com.xiaomi.aiasst.vision".equals(pkg)) {
            if (!Config.on(Config.K_G_AIASST, true)) return;
            if (!Config.on(Config.K_AIASST, false)) return;
            Logs.i("attach aiasst.vision");
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("xiaomixiaoai.com", "/speech/"));
            net.install(cl, "aiasst", r, false);
            return;
        }

        if ("com.miui.securitycenter".equals(pkg)) {
            if (!Config.on(Config.K_G_SECCENTER, true)) return;
            Logs.i("attach securitycenter");
            // 设备标识默认放行：安全中心要靠 IMSI / ICCID / 手机号查套餐与流量
            privacy.install(cl, pkg, false, false,
                    Config.on(Config.K_SC_IDS, false), false, false);
            if (Config.on(Config.K_XSOF, true)) xsof.install(cl);
            if (Config.on(Config.K_SC_DBGUARD, true)) {
                CaptureHooks.dbWrites(this, cl);
            }
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_SECCENTER, true)) {
                r.add(new NetGuard.Rule("fraud-detection.engine.miui.com"));
            }
            r.add(new NetGuard.Rule("flash.sec.miui.com", "/detect/"));
            if (Config.on(Config.K_SC_VIRUS, false)) r.add(new NetGuard.Rule("virus-engine.sec.miui.com"));
            if (Config.on(Config.K_SC_FRAMEWORK, false)) r.add(new NetGuard.Rule("framework.sec.miui.com"));
            if (Config.on(Config.K_SC_DATA, false)) r.add(new NetGuard.Rule("data.sec.miui.com"));
            if (Config.on(Config.K_SC_APPCLASS, false)) r.add(new NetGuard.Rule("flash.sec.miui.com", "/user/cat"));
            if (Config.on(Config.K_SC_VOLUNTEER, true)) {
                r.add(new NetGuard.Rule("api.sec.miui.com", "/earthquake/volunteer/sensor/upload"));
            }
            if (Config.on(Config.K_SC_LOCATION, false)) {
                r.add(new NetGuard.Rule("api.sec.miui.com", "/LBS/"));
                r.add(new NetGuard.Rule("api.sec.miui.com", "/earthquake/volunteer/"));
                r.add(new NetGuard.Rule("srv.sec.miui.com", "/earthquake/"));
                r.add(new NetGuard.Rule("gateway-papa.cloud.weizhi.com"));
                r.add(new NetGuard.Rule("eps.caict.ac.cn"));
            }
            if (Config.on(Config.K_SC_ADB, false)) r.add(new NetGuard.Rule("srv.sec.miui.com", "/data/adb"));
            net.install(cl, "securitycenter", r, false);
            return;
        }

        if ("com.xiaomi.registration".equals(pkg)) {
            if (!Config.on(Config.K_G_REGISTRATION, true)) return;
            Logs.i("attach registration");
            privacy.install(cl, pkg, false, false, true, true, false);
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_REG_ZZHC, false)) r.add(new NetGuard.Rule("zzhc.vnet.cn"));
            if (Config.on(Config.K_REG_TYPDM, true)) r.add(new NetGuard.Rule("typdm.tydevice.com"));
            if (r.isEmpty()) return;
            net.install(cl, "registration", r, false);
            return;
        }

        if ("com.miui.packageinstaller".equals(pkg)) {
            if (!Config.on(Config.K_G_PKG, true)) return;
            boolean pathOn = Config.on(Config.K_PATH, true);
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_PKG_AVL, true)) {
                r.add(new NetGuard.Rule("api.installer.xiaomi.com", "/avl/upload"));
            }
            if (pathOn && Config.on(Config.K_PKG_STAT, true)) {
                r.add(new NetGuard.Rule("api.installer.xiaomi.com", "/statistics/appactive"));
            }
            if (pathOn && Config.on(Config.K_AD_PKG, true)) {
                r.add(new NetGuard.Rule("api.installer.xiaomi.com", "/info/layout"));
            }
            if (r.isEmpty()) return;
            Logs.i("attach packageinstaller, rules=" + r.size());
            net.install(cl, "packageinstaller", r, false);
            return;
        }

        if ("com.xiaomi.market".equals(pkg)) {
            if (!Config.on(Config.K_G_AD_MARKET, true)) return;
            Logs.i("attach market");
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_AD_MARKET, true)) r.add(new NetGuard.Rule("sdkconfig.ad.xiaomi.com"));
            net.install(cl, "market", r, false);
            return;
        }

        if ("com.xiaomi.xmsf".equals(pkg)) {
            if (!Config.on(Config.K_G_AD_XMSF, true)) return;
            Logs.i("attach xmsf");
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_AD_XMSF, true)) r.add(new NetGuard.Rule("adv.sec.miui.com"));
            net.install(cl, "xmsf", r, false);
            return;
        }
    }
}
