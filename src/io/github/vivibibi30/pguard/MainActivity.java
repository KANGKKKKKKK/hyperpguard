package io.github.vivibibi30.pguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 卡片式开关面板。
 * 每组一个「启用」总开关（等同作用域开关），关闭时该组子项全部置灰不可操作。
 */
public class MainActivity extends Activity {

    private static final int BG = 0xFFF2F3F5;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF1A1A1A;
    private static final int SUB = 0xFF8A8A8E;
    private static final int LINE = 0xFFEFEFF0;
    private static final int ACCENT = 0xFF3478F6;

    private static final class Item {
        final String name, desc, key;
        final boolean def;

        Item(String name, String desc, String key, boolean def) {
            this.name = name;
            this.desc = desc;
            this.key = key;
            this.def = def;
        }
    }

    private static final class Group {
        final String title, pkg, key;
        final Item[] items;

        Group(String title, String pkg, String key, Item[] items) {
            this.title = title;
            this.pkg = pkg;
            this.key = key;
            this.items = items;
        }
    }

    /** 组总开关默认关闭：安装后需手动启用目标应用 */
    private static final Group[] GROUPS = {
            new Group("SystemHelper", "com.mobiletools.systemhelper", Config.K_G_SYSHELPER, new Item[]{
                    new Item("拦截上报", "dm.wo.com.cn，设备 / SIM / 基站 / 应用列表", Config.K_SYSHELPER, true),
            }),
            new Group("系统安全组件", "com.miui.guardprovider", Config.K_G_GUARD, new Item[]{
                    new Item("应用列表云查", "仅拦 flash.sec.miui.com，不动病毒库", Config.K_GUARD_APPLIST, true),
                    new Item("AI 通话预警云检测", "detection-aigc.engine.miui.com", Config.K_GUARD_AIGC, true),
                    new Item("反诈配置拉取", "fraud-detection.engine.miui.com", Config.K_GUARD_FRAUD, true),
            }),
            new Group("安全中心", "com.miui.securitycenter", Config.K_G_SECCENTER, new Item[]{
                    new Item("XSOF 行为查询拦截", "第三方查不到截图 / 通话 / 屏幕共享记录", Config.K_XSOF, true),
                    new Item("反诈配置拉取", "fraud-detection.engine.miui.com", Config.K_SECCENTER, true),
            }),
            new Group("小米统计", "com.miui.analytics", Config.K_G_ANALYTICS, new Item[]{
                    new Item("拦截 onetrack / mistat", "方法层置空 + 域名层拦截", Config.K_ANALYTICS, true),
                    new Item("严格模式", "该进程所有外网请求一并拦截", Config.K_STRICT, false),
            }),
            new Group("应用安装器", "com.miui.packageinstaller", Config.K_G_PKG, new Item[]{
                    new Item("AVL 上报", "…/avl/upload", Config.K_PKG_AVL, true),
                    new Item("安装活跃统计", "…/statistics/appactive", Config.K_PKG_STAT, true),
                    new Item("安装页广告位", "…/info/layout（AdModel）", Config.K_AD_PKG, true),
            }),
            new Group("应用商店", "com.xiaomi.market", Config.K_G_AD_MARKET, new Item[]{
                    new Item("广告配置下发", "sdkconfig.ad.xiaomi.com", Config.K_AD_MARKET, true),
            }),
            new Group("小米服务框架", "com.xiaomi.xmsf", Config.K_G_AD_XMSF, new Item[]{
                    new Item("广告接口", "adv.sec.miui.com", Config.K_AD_XMSF, true),
            }),
            new Group("小米可信服务", "com.xiaomi.trustservice", Config.K_G_TRUST, new Item[]{
                    new Item("屏蔽 MRM 认证链", "会影响设备证书 / 查找设备", Config.K_TRUST, false),
            }),
            new Group("翻译组件", "com.xiaomi.aiasst.vision", Config.K_G_AIASST, new Item[]{
                    new Item("语音识别上传", "只断 /speech/ 路径，保留文字翻译", Config.K_AIASST, false),
            }),
    };

    /** 组 key → 该组所有子项 Switch，用于整组置灰 / 恢复 */
    private final Map<String, List<Switch>> childSwitches = new LinkedHashMap<>();
    private final Map<String, List<LinearLayout>> childRows = new LinkedHashMap<>();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(18), dp(14), dp(24));
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("隐私上报拦截");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(TEXT);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("v" + Config.VERSION + " · LSPosed 模块");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(SUB);
        sub.setPadding(0, dp(4), 0, dp(10));
        root.addView(sub);

        TextView hint = new TextView(this);
        hint.setText("每个应用的「启用」开关即该应用的作用域开关，默认全部关闭。"
                + "开启后子项才可调整；关闭后该应用完全不注入。");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hint.setTextColor(Color.parseColor("#B07A00"));
        hint.setBackgroundColor(Color.parseColor("#FFF8E6"));
        hint.setPadding(dp(10), dp(8), dp(10), dp(8));
        hint.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dp(12);
        hint.setLayoutParams(hlp);
        root.addView(hint);

        // 环境检测占位（异步填充）
        final LinearLayout envBox = new LinearLayout(this);
        envBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(envBox);
        checkEnvironment(envBox);

        // 全局设置
        LinearLayout global = card();
        makeSwitchRow(global, "总开关", "关闭后本模块在所有进程不生效",
                Config.K_MASTER, true, true);
        global.addView(line());
        makeSwitchRow(global, "路径特征拦截", "路径规则（/avl/upload 等）是否生效",
                Config.K_PATH, true, true);
        global.addView(line());
        makeSwitchRow(global, "抓取模式", "记录「没被拦截」的访问，平时关掉",
                Config.K_SNIFF, false, true);
        global.addView(line());
        global.addView(buttonRow());
        root.addView(global);

        // 批量操作
        LinearLayout bulk = new LinearLayout(this);
        bulk.setOrientation(LinearLayout.HORIZONTAL);
        bulk.setPadding(0, 0, 0, dp(6));
        bulk.addView(bulkButton("全部启用", true));
        bulk.addView(bulkButton("全部停用", false));
        root.addView(bulk);

        // 每个应用一张卡片
        for (final Group g : GROUPS) {
            LinearLayout c = card();
            List<Switch> children = new ArrayList<>();
            List<LinearLayout> rows = new ArrayList<>();
            childSwitches.put(g.pkg, children);
            childRows.put(g.pkg, rows);

            c.addView(groupHeader(g, rows));
            for (Item it : g.items) {
                c.addView(line());
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(10), 0, dp(10));
                Switch sw = buildSwitchRow(row, it.name, it.desc, it.key, it.def, true);
                children.add(sw);
                rows.add(row);
                c.addView(row);
            }
            root.addView(c);
            applyGroupState(g, false);
        }

        Button disclaimer = new Button(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(4);
        disclaimer.setLayoutParams(dlp);
        disclaimer.setText("免责声明 / 使用条款");
        disclaimer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        disclaimer.setBackground(null);
        disclaimer.setTextColor(SUB);
        disclaimer.setOnClickListener(v -> showDisclaimer());
        root.addView(disclaimer);

        setContentView(sv);
    }

    /* ---------------- LSPosed 作用域同步 ---------------- */

    /**
     * 把某一组开关同步到 LSPosed 的作用域表。
     * 直接改 /data/adb/lspd/config/modules_config.db：先连同 -wal/-shm 复制出来，
     * 用 SQLiteDatabase 合并修改，再写回主库并清除旧 WAL，避免回滚覆盖。
     * 改完后新启动的进程即按新作用域注入；已运行进程需重启或重启设备。
     */
    private void syncScope(final String pkg, final boolean enable) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    File dir = new File(getCacheDir(), "lspd");
                    dir.mkdirs();
                    String src = "/data/adb/lspd/config/modules_config.db";
                    String dst = new File(dir, "modules_config.db").getAbsolutePath();

                    // 首次修改前保留原始备份
                    execSu("test -f " + src + ".pguard.bak || cp -f " + src + " " + src + ".pguard.bak");
                    execSu("cp -f " + src + " " + dst);
                    execSu("cp -f " + src + "-wal " + dst + "-wal 2>/dev/null; true");
                    execSu("cp -f " + src + "-shm " + dst + "-shm 2>/dev/null; true");
                    execSu("chmod 666 " + dst + "*");

                    SQLiteDatabase db = SQLiteDatabase.openDatabase(
                            dst, null, SQLiteDatabase.OPEN_READWRITE);
                    if (enable) {
                        db.execSQL("INSERT OR IGNORE INTO scope"
                                        + "(module_pkg_name,app_pkg_name,user_id) VALUES(?,?,0)",
                                new Object[]{Config.PKG, pkg});
                    } else {
                        db.execSQL("DELETE FROM scope WHERE module_pkg_name=? AND app_pkg_name=?",
                                new Object[]{Config.PKG, pkg});
                    }
                    db.close();

                    execSu("rm -f " + src + "-wal " + src + "-shm");
                    execSu("cp -f " + dst + " " + src);
                    execSu("chmod 666 " + src);

                    msg = (enable ? "已加入作用域：" : "已移出作用域：") + pkg
                            + "\n重启目标应用后生效";
                } catch (Throwable t) {
                    msg = "作用域同步失败：" + t + "\n可在 LSPosed 中手动勾选";
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void execSu(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Throwable ignored) {
        }
    }

    /* ---------------- 环境检测 ---------------- */

    private void checkEnvironment(final LinearLayout box) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg = null;
                boolean root = false;
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id -u"});
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    String line = br.readLine();
                    br.close();
                    p.waitFor();
                    root = line != null && "0".equals(line.trim());
                } catch (Throwable ignored) {
                }

                if (!root) {
                    msg = "本应用尚未获得 root 授权。请在 KernelSU / Magisk 中为「隐私上报拦截」授予超级用户权限，否则「重启应用」「查看记录」无法使用（拦截本身仍由 LSPosed 完成）。";
                } else {
                    boolean lsp = false;
                    try {
                        Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                                "test -d /data/adb/lspd && echo yes"});
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(p.getInputStream()));
                        String line = br.readLine();
                        br.close();
                        p.waitFor();
                        lsp = line != null && "yes".equals(line.trim());
                    } catch (Throwable ignored) {
                    }
                    if (!lsp) {
                        msg = "未检测到 LSPosed 运行环境。请安装 LSPosed，并在模块列表中启用本模块、勾选目标应用作用域。";
                    }
                }

                final String m = msg;
                if (m != null) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView tv = new TextView(MainActivity.this);
                            tv.setText("⚠ " + m);
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                            tv.setTextColor(Color.parseColor("#B3261E"));
                            tv.setBackgroundColor(Color.parseColor("#FDECEA"));
                            tv.setPadding(dp(10), dp(8), dp(10), dp(8));
                            tv.setLineSpacing(0, 1.15f);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            lp.bottomMargin = dp(10);
                            tv.setLayoutParams(lp);
                            box.addView(tv);
                        }
                    });
                }
            }
        }).start();
    }

    /* ---------------- 组状态联动 ---------------- */

    private void applyGroupState(Group g, boolean animate) {
        boolean on = Config.ui(this).getBoolean(g.key, false);
        List<Switch> children = childSwitches.get(g.pkg);
        List<LinearLayout> rows = childRows.get(g.pkg);
        if (children == null) return;
        for (int i = 0; i < children.size(); i++) {
            Switch sw = children.get(i);
            sw.setEnabled(on);
            if (rows != null && i < rows.size()) {
                rows.get(i).setAlpha(on ? 1f : 0.4f);
            }
        }
    }

    private android.view.View bulkButton(final String label, final boolean enable) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(ACCENT);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(8), 0, dp(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv.setOnClickListener(v -> {
            for (Group g : GROUPS) {
                Config.uiSet(this, g.key, enable);
                applyGroupState(g, false);
            }
            Toast.makeText(this, enable ? "已启用全部应用" : "已停用全部应用",
                    Toast.LENGTH_SHORT).show();
            recreate();
        });
        return tv;
    }

    /* ---------------- 卡片与控件 ---------------- */

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(16));
        c.setBackground(bg);
        c.setPadding(dp(16), dp(4), dp(16), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private ViewGroup line() {
        LinearLayout l = new LinearLayout(this);
        l.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        l.setBackgroundColor(LINE);
        return l;
    }

    private ViewGroup groupHeader(final Group g, List<LinearLayout> rows) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(g.title);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTypeface(null, Typeface.BOLD);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(g.pkg);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        TextView reboot = new TextView(this);
        reboot.setText("重启");
        reboot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        reboot.setTextColor(ACCENT);
        reboot.setPadding(dp(12), dp(6), dp(8), dp(6));
        reboot.setOnClickListener(v -> restartApp(g.pkg));
        row.addView(reboot);

        Switch sw = new Switch(this);
        sw.setChecked(Config.ui(this).getBoolean(g.key, false));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                Config.uiSet(MainActivity.this, g.key, checked);
                applyGroupState(g, true);
                syncScope(g.pkg, checked);
                Toast.makeText(MainActivity.this,
                        g.title + (checked ? " 已启用，正在同步作用域" : " 已停用，正在同步作用域"),
                        Toast.LENGTH_SHORT).show();
            }
        });
        row.addView(sw);
        return row;
    }

    /** 自建一行并挂到 parent，返回该 Switch */
    private Switch makeSwitchRow(LinearLayout parent, String name, String desc,
                                 final String key, boolean def, boolean enabled) {
        LinearLayout row = new LinearLayout(this);
        parent.addView(row);
        return buildSwitchRow(row, name, desc, key, def, enabled);
    }

    /** 把控件填进已有 row，返回该 Switch */
    private Switch buildSwitchRow(LinearLayout row, String name, String desc,
                                  final String key, boolean def, boolean enabled) {
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(name);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(desc);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        Switch sw = new Switch(this);
        sw.setChecked(Config.ui(this).getBoolean(key, def));
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                Config.uiSet(MainActivity.this, key, checked);
            }
        });
        row.addView(sw);
        return sw;
    }

    private ViewGroup buttonRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView view = new TextView(this);
        view.setText("查看记录");
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setTextColor(ACCENT);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(8), 0, dp(8));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        view.setOnClickListener(v -> loadLog(false));
        row.addView(view);

        TextView share = new TextView(this);
        share.setText("分享日志");
        share.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        share.setTextColor(ACCENT);
        share.setGravity(Gravity.CENTER);
        share.setPadding(0, dp(8), 0, dp(8));
        share.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        share.setOnClickListener(v -> loadLog(true));
        row.addView(share);

        return row;
    }

    private void restartApp(final String pkg) {
        Toast.makeText(this, "正在重启 " + pkg, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                            "am force-stop " + pkg});
                    int code = p.waitFor();
                    msg = code == 0 ? (pkg + " 已重启") : (pkg + " 重启失败 code=" + code);
                } catch (Throwable t) {
                    msg = "重启失败：" + t;
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /* ---------------- 日志 ---------------- */

    private void loadLog(final boolean share) {
        Toast.makeText(this, "读取中…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String log = execLogcat();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (share) doShare(log);
                        else showLog(log);
                    }
                });
            }
        }).start();
    }

    private String execLogcat() {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                    "logcat -d -v time -t 30000 | grep -F PrivacyGuard | tail -n 600"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            p.waitFor();
            if (sb.length() == 0) {
                return "没有记录。可能原因：\n"
                        + "1. root 未授权（su 弹窗没点允许）\n"
                        + "2. 模块刚启用，目标应用还没重启\n"
                        + "3. 对应应用的「启用」开关未打开\n"
                        + "4. 旧日志已被 logcat 环形缓冲覆盖";
            }
            return sb.toString();
        } catch (Throwable t) {
            return "读取失败：" + t;
        } finally {
            if (p != null) p.destroy();
        }
    }

    private void showLog(final String log) {
        TextView tv = new TextView(this);
        tv.setText(log);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("拦截记录（" + log.split("\n").length + " 行）")
                .setView(sv)
                .setPositiveButton("复制", (d, w) -> {
                    ClipboardManager cm =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("pg", log));
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("分享", (d, w) -> doShare(log))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void doShare(String log) {
        if (log == null || log.trim().isEmpty()) {
            Toast.makeText(this, "没有可分享的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "PrivacyGuard 拦截日志");
        i.putExtra(Intent.EXTRA_TEXT, log);
        startActivity(Intent.createChooser(i, "分享日志"));
    }

    /* ---------------- 免责声明 ---------------- */

    private void showDisclaimer() {
        String text;
        try {
            java.io.InputStream is = getAssets().open("DISCLAIMER.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            text = sb.toString();
        } catch (Throwable t) {
            text = "免责声明读取失败：" + t;
        }

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("免责声明")
                .setView(sv)
                .setPositiveButton("我知道了", null)
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
