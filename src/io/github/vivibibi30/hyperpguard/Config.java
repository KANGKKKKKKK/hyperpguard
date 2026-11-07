package io.github.vivibibi30.hyperpguard;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置。
 *
 * UI 侧通过普通 SharedPreferences 读写；hook 侧在新 API 下无法直接读应用私有目录，
 * 因此使用各规则的默认值（与 UI 的默认值一致）。细粒度开关由「应用是否在作用域内」
 * 加默认规则共同决定。
 */
public final class Config {

    public static final String PKG = "io.github.vivibibi30.hyperpguard";
    public static final String PREFS = "pg";
    public static final String VERSION = "1.3.0";

    /* 全局 */
    public static final String K_MASTER = "master";
    public static final String K_PATH = "block_path_markers";
    public static final String K_SNIFF = "sniff_all";

    /* 敏感数据：按应用独立，默认全部关闭 */
    public static final String K_LOC_SH = "loc_sh";       // SystemHelper 定位
    public static final String K_CELL_SH = "cell_sh";     // SystemHelper 基站
    public static final String K_WIFI_SH = "wifi_sh";     // SystemHelper Wi-Fi 扫描
    public static final String K_LOC_REG = "loc_reg";     // 运营商注册 定位
    public static final String K_CELL_REG = "cell_reg";   // 运营商注册 基站
    public static final String K_LOC_GP = "loc_gp";       // 反诈 定位
    public static final String K_WIFI_GP = "wifi_gp";     // 反诈 Wi-Fi 扫描
    public static final String K_SOCIAL_GP = "social_gp"; // 反诈 通讯录 / 通话
    public static final String K_LOC_SC = "loc_sc";       // 安全中心 定位
    public static final String K_SOCIAL_SC = "social_sc"; // 安全中心 通讯录 / 通话 / 短信
    public static final String K_CELL_DM = "cell_dm";     // 移动 DM 基站
    public static final String K_WIFI_DM = "wifi_dm";     // 移动 DM Wi-Fi 扫描

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

    /* trustservice */
    public static final String K_TRUST = "block_trustservice";

    /* analytics */
    public static final String K_ANALYTICS = "block_analytics";
    public static final String K_ANALYTICS_DOH = "block_analytics_doh";
    public static final String K_ANALYTICS_ALLNET = "analytics_all_net";
    public static final String K_SYSHELPER_ALLNET = "syshelper_all_net";

    /* 翻译组件 */
    public static final String K_AIASST = "block_aiasst";

    /* 安全中心 */
    public static final String K_SECCENTER = "block_securitycenter";
    public static final String K_SC_VIRUS = "block_sc_virus";
    public static final String K_SC_FRAMEWORK = "block_sc_framework";
    public static final String K_SC_DATA = "block_sc_data";
    public static final String K_SC_APPCLASS = "block_sc_appclass";
    public static final String K_SC_VOLUNTEER = "block_sc_volunteer";
    public static final String K_SC_LOCATION = "block_sc_location";
    public static final String K_SC_ADB = "block_sc_adb";
    public static final String K_SC_DBGUARD = "block_sc_dbguard";
    /** 安全中心的设备标识默认放行：拦了 IMSI / ICCID / 手机号，套餐与流量查询会失效 */
    public static final String K_SC_IDS = "block_sc_ids";
    public static final String K_GP_CLOUDSCAN = "block_gp_cloudscan";
    public static final String K_GP_SCREENGUARD = "block_gp_screenguard";
    public static final String K_TRUST_FIND = "block_trust_find";
    public static final String K_G_REGISTRATION = "g_registration";
    public static final String K_G_DMREG = "g_dmregservice";
    public static final String K_DMREG = "block_dmreg";
    public static final String K_REG_ZZHC = "block_reg_zzhc";
    public static final String K_REG_TYPDM = "block_reg_typdm";
    public static final String K_XSOF = "block_xsof";

    /* 安装器 */
    public static final String K_PKG_AVL = "block_packageinstaller_avl";
    public static final String K_PKG_STAT = "block_packageinstaller_stat";
    public static final String K_AD_PKG = "block_ad_pkg";

    /* 广告 */
    public static final String K_AD_MARKET = "block_ad_market";
    public static final String K_AD_XMSF = "block_ad_xmsf";

    private Config() {
    }

    /* ---------------- UI 侧 ---------------- */

    public static SharedPreferences ui(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void uiSet(Context ctx, String key, boolean v) {
        SharedPreferences sp = ui(ctx);
        sp.edit().putBoolean(key, v).apply();
        invalidateCache();
        syncOut(ctx, sp, key);
    }

    /**
     * 把开关写进 Settings.Global 的一个自定义 key，目标进程读它。
     *
     * 以 Settings 里已有的键值打底，再叠加本地偏好与本次改动：
     * 本地偏好被清理过时，不会把其余开关一起冲成默认值。
     */
    private static void syncOut(Context ctx, SharedPreferences sp, String changed) {
        java.util.LinkedHashMap<String, Boolean> map = new java.util.LinkedHashMap<>();
        String cur = null;
        try {
            cur = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
        } catch (Throwable t) {
            Logs.i("config read before sync fail: " + t);
        }
        if (cur != null && !cur.isEmpty()) {
            for (String kv : cur.split("\\|")) {
                int i = kv.indexOf('=');
                if (i <= 0) continue;
                map.put(kv.substring(0, i).trim(), "true".equals(kv.substring(i + 1).trim()));
            }
        }
        for (java.util.Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            Object v = e.getValue();
            if (v instanceof Boolean) map.put(e.getKey(), (Boolean) v);
        }
        map.put(changed, sp.getBoolean(changed, false));

        final StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Boolean> e : map.entrySet()) {
            if (sb.length() > 0) sb.append('|');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        if (sb.length() == 0) return;
        final String dump = sb.toString();
        new Thread(() -> {
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                        "settings put global " + GLOBAL_KEY + " '" + dump + "'"});
                p.waitFor();
            } catch (Throwable t) {
                Logs.i("config sync fail: " + t);
            } finally {
                if (p != null) try { p.destroy(); } catch (Throwable ignored) { }
            }
        }, "hyperpguard-config-sync").start();
    }

    /** 界面读取开关：本地没有时回落到 Settings，清过数据也不会显示成默认值 */
    public static boolean get(Context ctx, String key, boolean def) {
        SharedPreferences sp = ui(ctx);
        if (sp.contains(key)) return sp.getBoolean(key, def);
        Boolean remote = fallback(ctx).get(key);
        return remote != null ? remote : def;
    }

    /** Settings 里的整表，只在本地缺键时载入一次 */
    private static java.util.Map<String, Boolean> fallback(Context ctx) {
        if (FALLBACK != null) return FALLBACK;
        java.util.LinkedHashMap<String, Boolean> map = new java.util.LinkedHashMap<>();
        try {
            String v = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
            if (v != null && !v.isEmpty()) {
                for (String kv : v.split("\\|")) {
                    int i = kv.indexOf('=');
                    if (i <= 0) continue;
                    map.put(kv.substring(0, i).trim(), "true".equals(kv.substring(i + 1).trim()));
                }
            }
        } catch (Throwable t) {
            Logs.i("config fallback fail: " + t);
        }
        FALLBACK = map;
        return FALLBACK;
    }

    private static java.util.Map<String, Boolean> FALLBACK;

    /** 界面改过开关后丢掉回落缓存，下次重新读 */
    public static void invalidateCache() {
        FALLBACK = null;
    }

    /* ---------------- Hook 侧 ---------------- */

    public static boolean masterOn() {
        return on(K_MASTER, true);
    }

    /** 唯一的对外写入：Settings.Global 下的一个自定义 key */
    public static final String GLOBAL_KEY = "hyperpguard_conf";

    /** hook 侧读到的开关，进程内缓存一份，读不到就用各规则的默认值 */
    private static final java.util.Map<String, Boolean> REMOTE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 目标进程读取界面开关：查询本应用的只读 provider。
     * 不写文件、不写系统设置，查询失败时按默认值运行。
     */
    public static void load(android.content.Context ctx) {
        if (!REMOTE.isEmpty()) return;
        if (ctx != null) loadFromSettings(ctx);
        Logs.i(REMOTE.isEmpty() ? "config loaded: 0 (defaults)"
                : "config loaded: " + REMOTE.size());
    }

    private static void loadFromSettings(android.content.Context ctx) {
        try {
            String v = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
            if (v == null || v.isEmpty()) return;
            for (String kv : v.split("\\|")) {
                int i = kv.indexOf('=');
                if (i <= 0) continue;
                REMOTE.put(kv.substring(0, i).trim(), "true".equals(kv.substring(i + 1).trim()));
            }
        } catch (Throwable t) {
            Logs.i("config read fail: " + t);
        }
    }

    public static int size() {
        return REMOTE.size();
    }

    public static boolean on(String key, boolean fallback) {
        Boolean v = REMOTE.get(key);
        return v == null ? fallback : v;
    }

    public static boolean strict() {
        return false;
    }
}
