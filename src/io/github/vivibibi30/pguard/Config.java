package io.github.vivibibi30.pguard;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 配置读写：UI 写 SharedPreferences，hook 侧用 XSharedPreferences 读。
 * 读不到配置时按拦截处理（fail-closed）。
 */
public final class Config {

    public static final String PKG = "io.github.vivibibi30.pguard";
    public static final String PREFS = "pg";
    public static final String VERSION = "1.0.0";

    public static final String K_MASTER = "master";

    /* 路径规则总开关（控制带 path 的精确规则是否生效） */
    public static final String K_PATH = "block_path_markers";

    /* 抓取模式：记录未拦截的访问 */
    public static final String K_SNIFF = "sniff_all";

    /* 分组总开关 */
    public static final String K_G_SYSHELPER = "g_syshelper";
    public static final String K_G_GUARD = "g_guardprovider";
    public static final String K_G_TRUST = "g_trustservice";
    public static final String K_G_ANALYTICS = "g_analytics";
    public static final String K_G_AIASST = "g_aiasst";
    public static final String K_G_SECCENTER = "g_seccenter";
    public static final String K_G_PKG = "g_packageinstaller";
    public static final String K_G_AD_MARKET = "g_ad_market";
    public static final String K_G_AD_XMSF = "g_ad_xmsf";

    /* SystemHelper */
    public static final String K_SYSHELPER = "block_syshelper";

    /* guardprovider */
    public static final String K_GUARD_APPLIST = "block_guard_applist";
    public static final String K_GUARD_AIGC = "block_guard_aigc";
    public static final String K_GUARD_FRAUD = "block_guard_fraud";

    /* trustservice（默认关：会影响设备证书/查找设备） */
    public static final String K_TRUST = "block_trustservice";

    /* analytics（默认关） */
    public static final String K_ANALYTICS = "block_analytics";
    public static final String K_STRICT = "strict_all";

    /* 翻译 / AI 通话识别后端（默认关） */
    public static final String K_AIASST = "block_aiasst";

    /* securitycenter */
    public static final String K_SECCENTER = "block_securitycenter";
    public static final String K_XSOF = "block_xsof";

    /* 安装器 */
    public static final String K_PKG_AVL = "block_packageinstaller_avl";
    public static final String K_PKG_STAT = "block_packageinstaller_stat";

    /* 广告（域名均为实测确认） */
    public static final String K_AD_MARKET = "block_ad_market";
    public static final String K_AD_XMSF = "block_ad_xmsf";
    public static final String K_AD_PKG = "block_ad_pkg";

    private Config() {
    }

    /* ---------------- UI 侧 ---------------- */

    public static SharedPreferences ui(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void uiSet(Context ctx, String key, boolean v) {
        ui(ctx).edit().putBoolean(key, v).apply();
    }

    /* ---------------- Hook 侧 ---------------- */

    private static XSharedPreferences prefs;

    private static XSharedPreferences prefs() {
        if (prefs == null) {
            try {
                prefs = new XSharedPreferences(PKG, PREFS);
            } catch (Throwable t) {
                prefs = null;
            }
        }
        return prefs;
    }

    private static Boolean read(String key) {
        try {
            XSharedPreferences p = prefs();
            if (p == null) return null;
            File f = p.getFile();
            if (f == null || !f.exists()) return null;
            return p.getBoolean(key, true);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean masterOn(XC_LoadPackage.LoadPackageParam lp) {
        Boolean v = read(K_MASTER);
        return v == null || v;
    }

    /**
     * 读开关。注意：读不到配置时返回调用方给的 fallback，
     * 但 UI 侧已经为每项设置了显式默认值，所以这里以 UI 的默认值为准。
     */
    public static boolean on(XC_LoadPackage.LoadPackageParam lp, String key, boolean fallback) {
        Boolean v = read(key);
        return v == null ? fallback : v;
    }

    public static boolean strict(XC_LoadPackage.LoadPackageParam lp) {
        Boolean v = read(K_STRICT);
        return v != null && v;
    }

    public static void log(String s) {
        try {
            XposedBridge.log("[PrivacyGuard] " + s);
        } catch (Throwable ignored) {
        }
    }
}
