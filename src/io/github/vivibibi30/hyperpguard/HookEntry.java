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

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        final String pkg = param.getPackageName();
        final ClassLoader cl = param.getClassLoader();
        if (pkg == null || cl == null) return;
        if (!Config.masterOn()) return;

        try {
            dispatch(pkg, cl);
        } catch (Throwable t) {
            Logs.i("dispatch fail " + pkg + ": " + t);
        }
    }

    private void dispatch(String pkg, ClassLoader cl) {
        if ("com.mobiletools.systemhelper".equals(pkg)) {
            if (!Config.on(Config.K_G_SYSHELPER, true)) return;
            Logs.i("attach systemhelper");
            privacy.install(cl, false, false, true, false, false);
            Hooks.ret(this, cl, "c.d.a.b.f", "a",
                    new Class<?>[]{String.class, byte[].class, byte[].class}, Boolean.FALSE);
            Hooks.ret(this, cl, "c.d.a.b.e", "a", new Class<?>[]{String.class}, null);
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("dm.wo.com.cn"));
            net.install(cl, "systemhelper", r, Config.on(Config.K_SYSHELPER_ALLNET, true));
            return;
        }

        if ("com.miui.guardprovider".equals(pkg)) {
            List<NetGuard.Rule> r = new ArrayList<>();
            if (Config.on(Config.K_GUARD_APPLIST, true)) r.add(new NetGuard.Rule("flash.sec.miui.com"));
            if (Config.on(Config.K_GUARD_AIGC, true)) r.add(new NetGuard.Rule("detection-aigc.engine.miui.com"));
            if (Config.on(Config.K_GUARD_FRAUD, true)) r.add(new NetGuard.Rule("fraud-detection.engine.miui.com"));
            if (Config.on(Config.K_GP_CLOUDSCAN, false)) r.add(new NetGuard.Rule("virus-engine.sec.miui.com"));
            if (r.isEmpty()) return;
            Logs.i("attach guardprovider, rules=" + r.size());
            privacy.install(cl, true, false, true, false, false);
            if (Config.on(Config.K_GP_SCREENGUARD, true)) {
                CaptureHooks.screenCapture(this, cl);
            }
            net.install(cl, "guardprovider", r, false);
            return;
        }

        if ("com.xiaomi.trustservice".equals(pkg)) {
            if (!Config.on(Config.K_G_TRUST, true)) return;
            Logs.i("attach trustservice");
            xsof.mrm(cl);
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("deviceapi.micloud.xiaomi.net"));
            if (Config.on(Config.K_TRUST_FIND, false)) {
                r.add(new NetGuard.Rule("find.api.micloud.xiaomi.net"));
            }
            net.install(cl, "trustservice", r, false);
            return;
        }

        if ("com.miui.analytics".equals(pkg)) {
            if (!Config.on(Config.K_ANALYTICS, true)) return;
            Logs.i("attach analytics");
            analytics.install(cl);
            privacy.install(cl, true, true, true, false, true);
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
            privacy.install(cl, false, false, true, false, false);
            if (Config.on(Config.K_XSOF, true)) xsof.install(cl);
            if (Config.on(Config.K_SC_DBGUARD, true)) {
                CaptureHooks.dbWrites(this, cl);
            }
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("fraud-detection.engine.miui.com"));
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
            privacy.install(cl, false, false, true, true, false);
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
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("sdkconfig.ad.xiaomi.com"));
            net.install(cl, "market", r, false);
            return;
        }

        if ("com.xiaomi.xmsf".equals(pkg)) {
            if (!Config.on(Config.K_G_AD_XMSF, true)) return;
            List<NetGuard.Rule> r = new ArrayList<>();
            r.add(new NetGuard.Rule("adv.sec.miui.com"));
            net.install(cl, "xmsf", r, false);
        }
    }
}
