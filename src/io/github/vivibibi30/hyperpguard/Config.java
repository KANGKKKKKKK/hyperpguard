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
    public static final String VERSION = "1.2.6";

    /* 全局 */
    public static final String K_MASTER = "master";
    public static final String K_PATH = "block_path_markers";
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

    /* trustservice */
    public static final String K_TRUST = "block_trustservice";

    /* analytics */
    public static final String K_ANALYTICS = "block_analytics";
    public static final String K_STRICT = "strict_all";
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
    public static final String K_GP_CLOUDSCAN = "block_gp_cloudscan";
    public static final String K_GP_SCREENGUARD = "block_gp_screenguard";
    public static final String K_TRUST_FIND = "block_trust_find";
    public static final String K_G_REGISTRATION = "g_registration";
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
        syncToGlobal(sp);
    }

    /** 打开界面时主动重建一次配置文件，防止被清理工具删掉后开关回落到默认值 */
    public static void syncNow(Context ctx) {
        syncToGlobal(ui(ctx));
    }

    private static void syncToGlobal(final SharedPreferences sp) {
        final StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            if (sb.length() > 0) sb.append('|');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        final String dump = sb.toString();
        new Thread(() -> {
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                        "settings put global " + GLOBAL_KEY + " '" + dump + "'"});
                p.waitFor();
                Logs.i("config synced to Settings.Global");
            } catch (Throwable t) {
                Logs.i("config sync fail: " + t);
            } finally {
                if (p != null) try { p.destroy(); } catch (Throwable ignored) { }
            }
        }, "hyperpguard-config-sync").start();
    }

    /* ---------------- Hook 侧 ---------------- */

    public static boolean masterOn() {
        return on(K_MASTER, true);
    }

    /** 开关存放在 Settings.Global，目标进程无需文件、无需权限即可读到 */
    public static final String GLOBAL_KEY = "hyperpguard_conf";

    /** 兼容旧版本留下的文件，只读不写 */
    public static final String CONF_FILE = "/data/local/tmp/hyperpguard.conf";

    /** hook 侧读到的开关，进程内缓存一份，读不到就用各规则的默认值 */
    private static final java.util.Map<String, Boolean> REMOTE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 在目标进程里尝试一次性拉取 UI 侧的开关。
     * 没有 Context、provider 不存在、被拦截，都只是回退到默认值，不影响拦截本身。
     */
    public static void load(android.content.Context ctx) {
        if (!REMOTE.isEmpty()) return;
        if (ctx != null) {
            try {
                String v = android.provider.Settings.Global.getString(
                        ctx.getContentResolver(), GLOBAL_KEY);
                if (v != null && !v.isEmpty()) parse(v);
            } catch (Throwable t) {
                Logs.i("config settings fail: " + t);
            }
        }
        if (!REMOTE.isEmpty()) {
            Logs.i("config loaded: " + REMOTE.size() + " (settings)");
            return;
        }
        android.database.Cursor c = null;
        try {
            c = ctx.getContentResolver().query(ConfigProvider.URI, null, null, null, null);
            if (c == null) return;
            while (c.moveToNext()) {
                String k = c.getString(0);
                if (k == null) continue;
                REMOTE.put(k, "true".equals(c.getString(1)));
            }
        } catch (Throwable t) {
            Logs.i("config query skipped: " + t);
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) { }
        }
        if (REMOTE.isEmpty()) loadFromFile();
        Logs.i(REMOTE.isEmpty() ? "config loaded: 0 (use built-in defaults)"
                : "config loaded: " + REMOTE.size() + " (file)");
    }

    private static void parse(String v) {
        for (String kv : v.split("\\|")) {
            int i = kv.indexOf('=');
            if (i <= 0) continue;
            REMOTE.put(kv.substring(0, i).trim(), "true".equals(kv.substring(i + 1).trim()));
        }
    }

    /** 普通应用进程查 provider 会被 AMS 以「包名与 uid 不符」拒绝，改读这份文件 */
    private static void loadFromFile() {
        java.io.BufferedReader br = null;
        try {
            br = new java.io.BufferedReader(new java.io.FileReader(CONF_FILE));
            String line;
            while ((line = br.readLine()) != null) {
                int i = line.indexOf('=');
                if (i <= 0) continue;
                REMOTE.put(line.substring(0, i).trim(), "true".equals(line.substring(i + 1).trim()));
            }
        } catch (Throwable t) {
            Logs.i("config file skip: " + t);
        } finally {
            if (br != null) try { br.close(); } catch (Throwable ignored) { }
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
