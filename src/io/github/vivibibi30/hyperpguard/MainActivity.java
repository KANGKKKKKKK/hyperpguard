package io.github.vivibibi30.hyperpguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import java.util.Collections;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
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

    private static final String URL_REPO = "https://github.com/vivibibi30/hyperpguard";
    private static final String URL_ISSUES = URL_REPO + "/issues";
    private static final String URL_RELEASES = URL_REPO + "/releases";
    private static final String URL_MODULE =
            "https://modules.lsposed.org/module/io.github.vivibibi30.hyperpguard";
    private static final String API_LATEST =
            "https://api.github.com/repos/vivibibi30/hyperpguard/releases/latest";

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
        final String title, pkg, key, note;
        final Item[] items;

        Group(String title, String pkg, String key, Item[] items) {
            this(title, pkg, key, items, null);
        }

        Group(String title, String pkg, String key, Item[] items, String note) {
            this.title = title;
            this.pkg = pkg;
            this.key = key;
            this.items = items;
            this.note = note;
        }
    }

    /** 组总开关默认开启：状态由 LSPosed 真实作用域回填，不看本地默认值 */
    private static final Group[] GROUPS = {
            new Group("SystemHelper", "com.mobiletools.systemhelper", Config.K_G_SYSHELPER, new Item[]{
                    new Item("拦截上报", "dm.wo.com.cn，设备 / SIM / 基站 / 应用列表", Config.K_SYSHELPER, true),
                    new Item("全断网（推荐）", "该进程所有外网请求一并拦截，只留本机回环；它没有用户功能", Config.K_SYSHELPER_ALLNET, true),
                    new Item("定位（GPS）", "只对本应用：位置查询返回空", Config.K_LOC_SH, false),
                    new Item("基站信息", "只对本应用：基站查询返回空，不影响信号显示", Config.K_CELL_SH, false),
                    new Item("Wi-Fi 扫描", "只对本应用：扫描列表置空，SSID / BSSID 清空", Config.K_WIFI_SH, false),
            }, "权限在开机激活时确认；本模块只拦上报，不回收已授予的权限"),
            new Group("系统安全组件", "com.miui.guardprovider", Config.K_G_GUARD, new Item[]{
                    new Item("应用列表云查", "仅拦 flash.sec.miui.com，不动病毒库", Config.K_GUARD_APPLIST, true),
                    new Item("AI 通话预警云检测", "detection-aigc.engine.miui.com", Config.K_GUARD_AIGC, true),
                    new Item("反诈配置拉取", "fraud-detection.engine.miui.com", Config.K_GUARD_FRAUD, true),
                    new Item("云查上传（Flamingo）", "virus-engine.sec.miui.com；关闭后不再上传应用清单，本地病毒库照常", Config.K_GP_CLOUDSCAN, false),
                    new Item("静默录屏拦截", "阻止 createVirtualDisplay 整屏录制，不影响正常截屏", Config.K_GP_SCREENGUARD, true),
                    new Item("定位（GPS）", "只对本应用：位置查询返回空", Config.K_LOC_GP, false),
                    new Item("Wi-Fi 扫描", "只对本应用：扫描列表置空，SSID / BSSID 清空", Config.K_WIFI_GP, false),
                    new Item("通讯录 / 通话读取", "只对本应用：查询返回空；来电识别会受影响", Config.K_SOCIAL_GP, false),
            }, "AI 反诈逻辑在本组件的 AiGuardService 内，用于通话场景；SystemHelper 只做运营商侧上报"),
            new Group("安全中心", "com.miui.securitycenter", Config.K_G_SECCENTER, new Item[]{
                    new Item("XSOF 行为查询拦截", "第三方查不到截图 / 通话 / 屏幕共享记录", Config.K_XSOF, true),
                    new Item("反诈配置拉取", "fraud-detection.engine.miui.com", Config.K_SECCENTER, true),
                    new Item("病毒库云查", "virus-engine.sec.miui.com，关闭后不再云端查毒", Config.K_SC_VIRUS, false),
                    new Item("XSOF 合作方鉴权", "framework.sec.miui.com，可能影响第三方调用", Config.K_SC_FRAMEWORK, false),
                    new Item("月报与配置上报", "data.sec.miui.com，含防窥/广告等接口", Config.K_SC_DATA, false),
                    new Item("防窥分类上报", "上传非系统应用包名数组；关闭后防窥识别可能变差", Config.K_SC_APPCLASS, false),
                    new Item("志愿者传感器上报", "OAID + 经纬度 + 时间；非志愿者无影响", Config.K_SC_VOLUNTEER, true),
                    new Item("地震/报警/EPS 定位", "关闭后地震预警、紧急报警定位可能失效", Config.K_SC_LOCATION, false),
                    new Item("ADB 授权验证", "srv.sec.miui.com/data/adb；关闭后 ADB 安装授权可能失败", Config.K_SC_ADB, false),
                    new Item("行为库写入拦截", "拦截 screenshot / screen_share / voip_record 等 8 张表的写入", Config.K_SC_DBGUARD, true),
                    new Item("设备标识拦截", "IMSI / ICCID / 手机号 / IMEI；打开后剩余流量、套餐查询会失效", Config.K_SC_IDS, false),
                    new Item("定位（GPS）", "只对本应用：位置查询返回空", Config.K_LOC_SC, false),
                    new Item("通讯录 / 通话 / 短信读取", "只对本应用：查询返回空；来电识别会受影响", Config.K_SOCIAL_SC, false),
            }),
            new Group("小米统计", "com.miui.analytics", Config.K_G_ANALYTICS, new Item[]{
                    new Item("拦截上报", "tracking / om.tracking / sdkconfig", Config.K_ANALYTICS, true),
                    new Item("阻断 DoH 旁路", "dns.google 与 8.8.8.8，防解析绕过域名规则", Config.K_ANALYTICS_DOH, true),
                    new Item("全断网（推荐）", "该进程所有外网请求一并拦截；云端换域名也不用管", Config.K_ANALYTICS_ALLNET, true),
            }),
            new Group("应用安装器", "com.miui.packageinstaller", Config.K_G_PKG, new Item[]{
                    new Item("AVL 上报", "…/avl/upload", Config.K_PKG_AVL, true),
                    new Item("安装活跃统计", "…/statistics/appactive", Config.K_PKG_STAT, true),
                    new Item("安装页广告位", "…/info/layout（AdModel）", Config.K_AD_PKG, true),
            }),
            new Group("应用商店", "com.xiaomi.market", Config.K_G_AD_MARKET, new Item[]{
                    new Item("广告配置下发", "sdkconfig.ad.xiaomi.com", Config.K_AD_MARKET, true),
                    new Item("使用统计上报", "tracking.miui.com/track；只影响统计，不影响下载与安装", Config.K_MARKET_TRACK, true),
            }),
            new Group("小米服务框架", "com.xiaomi.xmsf", Config.K_G_AD_XMSF, new Item[]{
                    new Item("广告接口", "adv.sec.miui.com", Config.K_AD_XMSF, true),
                    new Item("使用统计上报", "tracking.miui.com/track；只影响统计，不影响推送与登录", Config.K_XMSF_TRACK, true),
            }),
            new Group("运营商服务", "com.xiaomi.registration", Config.K_G_REGISTRATION, new Item[]{
                    new Item("设备注册（zzhc）", "IMEI/IMSI/ICCID；关闭后 VoLTE / 运营商功能可能异常", Config.K_REG_ZZHC, false),
                    new Item("明文 CTEI 上报", "http://typdm.tydevice.com，IMEI / MAC / BSSID 明文 GET", Config.K_REG_TYPDM, true),
                    new Item("定位（GPS）", "只对本应用：位置查询返回空", Config.K_LOC_REG, false),
                    new Item("基站信息", "只对本应用：基站查询返回空，不影响信号显示", Config.K_CELL_REG, false),
            }),
            new Group("移动 DM（RegService）", "com.miui.dmregservice", Config.K_G_DMREG, new Item[]{
                    new Item("拦截上报", "fxltsbl.com；App 清单 / SIM 标识", Config.K_DMREG, true),
                    new Item("基站信息", "只对本应用：基站查询返回空，不影响信号显示", Config.K_CELL_DM, false),
                    new Item("Wi-Fi 扫描", "只对本应用：扫描列表置空，SSID / BSSID 清空", Config.K_WIFI_DM, false),
            }),
            new Group("系统诊断（MiSight）", "com.miui.misightservice", Config.K_G_MISIGHT, new Item[]{
                    new Item("日志上传", "feedback.miui.com/misight/upload；关闭后主动反馈传不了日志包", Config.K_MISIGHT_UPLOAD, true),
                    new Item("售后检测", "port.sec.miui.com/mqsas；关闭后售后远程检测拿不到日志", Config.K_MISIGHT_MQSAS, true),
                    new Item("客服诊断", "chat.kefu.mi.com/openapi/misight/diagnostic；关闭后在线客服远程诊断不可用", Config.K_MISIGHT_KEFU, true),
            }, "触发条件是用户主动反馈或售后远程诊断；本地记录不等于已经上传"),
            new Group("小米可信服务", "com.xiaomi.trustservice", Config.K_G_TRUST, new Item[]{
                    new Item("屏蔽 MRM 认证链", "会影响设备证书 / 查找设备", Config.K_TRUST, false),
                    new Item("设备证书/密钥上报", "find.api.micloud.xiaomi.net；关闭后查找设备、设备证书可能异常", Config.K_TRUST_FIND, false),
            }),
            new Group("翻译组件", "com.xiaomi.aiasst.vision", Config.K_G_AIASST, new Item[]{
                    new Item("语音识别上传", "只断 /speech/ 路径，保留文字翻译", Config.K_AIASST, false),
            }),
    };

    /** 组 key → 该组所有子项 Switch / 行，用于刷新真实状态（不置灰） */
    private final Map<String, List<Switch>> childSwitches = new LinkedHashMap<>();
    private final Map<String, List<LinearLayout>> childRows = new LinkedHashMap<>();
    /** 组 key → 组开关 */
    private final Map<String, Switch> groupSwitches = new LinkedHashMap<>();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private XposedService xposed;
    private boolean updatingSwitch = false;
    private int bindRetry = 0;
    private boolean autoRequested = false;

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
        title.setText("HyperPGuard");
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

        // 顶部明确语义：开关打开是拦截，不是放行
        TextView hint = new TextView(this);
        hint.setText("开关打开 = 拦截该行为，关闭 = 放行");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hint.setTextColor(0xFF1A6FD4);
        GradientDrawable hintBg = new GradientDrawable();
        hintBg.setColor(0xFFE8F2FF);
        hintBg.setCornerRadius(dp(10));
        hint.setBackground(hintBg);
        hint.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.bottomMargin = dp(12);
        root.addView(hint, hintLp);

        // 全局设置
        LinearLayout global = card();
        makeSwitchRow(global, "总开关", "关闭后本模块在所有进程不生效",
                Config.K_MASTER, true, true);
        global.addView(line());
        makeSwitchRow(global, "路径特征拦截", "路径规则（/avl/upload 等）是否生效",
                Config.K_PATH, true, true);
        global.addView(line());
        makeSwitchRow(global, "记录未拦截访问", "把没命中规则的域名写进日志，排查漏拦时临时打开",
                Config.K_SNIFF, false, true);
        global.addView(line());
        addHideIconRow(global);
        global.addView(line());
        addFraudDbRow(global);
        global.addView(line());
        // 需要 root 的功能，提示写在这里
        TextView rootTip = new TextView(this);
        rootTip.setText("「重启」「查看记录」需要 root，首次使用请允许 KernelSU 授权");
        rootTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        rootTip.setTextColor(SUB);
        rootTip.setPadding(0, dp(8), 0, 0);
        global.addView(rootTip);
        global.addView(buttonRow());
        root.addView(global);

        // 批量操作
        LinearLayout bulk = new LinearLayout(this);
        bulk.setOrientation(LinearLayout.HORIZONTAL);
        bulk.setPadding(0, 0, 0, dp(6));
        bulk.addView(bulkButton("全部启用", this::requestAll));
        bulk.addView(bulkButton("全部停用", this::removeAll));
        bulk.addView(bulkButton("全部重启", this::restartAll));
        root.addView(bulk);



        // 每个应用一张卡片
        for (final Group g : GROUPS) {
            LinearLayout c = card();
            List<Switch> children = new ArrayList<>();
            List<LinearLayout> rows = new ArrayList<>();
            childSwitches.put(g.pkg, children);
            childRows.put(g.pkg, rows);

            c.addView(groupHeader(g, rows));
            if (g.note != null) c.addView(groupNote(g.note));
            for (Item it : g.items) {
                c.addView(line());
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(10), 0, dp(10));
                Switch sw = buildSwitchRow(row, it.name, it.desc, it.key, it.def, true, g);
                children.add(sw);
                rows.add(row);
                c.addView(row);
            }
            root.addView(c);
            setGroupUi(g, Config.get(this, g.key, true));
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

        // 关于
        LinearLayout about = card();
        TextView aboutHead = new TextView(this);
        aboutHead.setText("关于");
        aboutHead.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        aboutHead.setTextColor(SUB);
        aboutHead.setPadding(0, dp(12), 0, 0);
        about.addView(aboutHead);
        about.addView(linkRow("GitHub 仓库", "vivibibi30/hyperpguard", URL_REPO));
        about.addView(line());
        about.addView(linkRow("LSPosed 模块页", "modules.lsposed.org", URL_MODULE));
        about.addView(line());
        about.addView(linkRow("问题反馈", "Issues", URL_ISSUES));
        about.addView(line());
        Button checkUpdateBtn = new Button(this);
        checkUpdateBtn.setText("检查更新");
        checkUpdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        checkUpdateBtn.setBackground(null);
        checkUpdateBtn.setTextColor(ACCENT);
        checkUpdateBtn.setAllCaps(false);
        checkUpdateBtn.setPadding(0, dp(10), 0, dp(10));
        checkUpdateBtn.setOnClickListener(v -> checkUpdate());
        about.addView(checkUpdateBtn);
        root.addView(about);

        root.addView(disclaimer);

        bindServiceHelper();

        setContentView(sv);
    }

    /** LSPosed 服务断开后重连：覆盖安装模块时服务端会重启一次 */
    private void bindServiceHelper() {
        try {
            XposedServiceHelper.registerListener(serviceListener);
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "registerListener fail", t);
        }
    }

    private final XposedServiceHelper.OnServiceListener serviceListener =
            new XposedServiceHelper.OnServiceListener() {
                @Override
                public void onServiceBind(XposedService service) {
                    xposed = service;
                    bindRetry = 0;
                    android.util.Log.i("HyperPGuard", "XposedService bound: "
                            + service.getFrameworkName() + " " + service.getFrameworkVersion());
                    ui.post(MainActivity.this::syncFromService);
                }

                @Override
                public void onServiceDied(XposedService service) {
                    xposed = null;
                    android.util.Log.w("HyperPGuard", "XposedService died");
                    if (bindRetry++ < 5) {
                        ui.postDelayed(MainActivity.this::bindServiceHelper, 1500);
                    }
                }
            };

    @Override
    protected void onResume() {
        super.onResume();
        if (xposed != null) syncFromService();
    }

    /* ---------------- 软重启 ---------------- */

    private void requestAdd(final String pkg, final Group g, final Switch sw) {
        if (xposed == null) {
            Toast.makeText(this, "LSPosed 服务未连接，请确认已在 LSPosed 中启用本模块",
                    Toast.LENGTH_LONG).show();
            return;
        }
        android.util.Log.i("HyperPGuard", "requestScope(" + pkg + ") sending");
        try {
            xposed.requestScope(Collections.singletonList(pkg),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(java.util.List<String> approved) {
                            ui.post(() -> commitState(g, sw, true,
                                    "已加入作用域：" + approved));
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            android.util.Log.w("HyperPGuard", "requestScope(" + pkg + ") failed: " + message);
                            ui.post(() -> showScopeFailure("「" + g.title + "」加入作用域失败",
                                    pkg, message));
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private void requestRemove(final String pkg, final Group g, final Switch sw) {
        if (xposed == null) return;
        try {
            xposed.removeScope(Collections.singletonList(pkg));
            commitState(g, sw, false, "已移出作用域：" + pkg);
        } catch (Throwable t) {
            Toast.makeText(this, "移除失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** 一次性请求全部作用域 */
    private void requestAll() {
        if (xposed == null) {
            Toast.makeText(this, "LSPosed 服务未连接", Toast.LENGTH_LONG).show();
            return;
        }
        final java.util.List<String> all = new java.util.ArrayList<>();
        for (Group g : GROUPS) {
            if (isInstalled(g.pkg)) all.add(g.pkg);
        }
        if (all.isEmpty()) {
            Toast.makeText(this, "没有可加入的目标应用", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            xposed.requestScope(all, new XposedService.OnScopeEventListener() {
                @Override
                public void onScopeRequestApproved(java.util.List<String> approved) {
                    ui.post(() -> {
                        for (Group g : GROUPS) {
                            if (approved != null && approved.contains(g.pkg)) {
                                Config.uiSet(MainActivity.this, g.key, true);
                            }
                        }
                        Toast.makeText(MainActivity.this,
                                "已加入 " + (approved == null ? 0 : approved.size())
                                        + " 个作用域", Toast.LENGTH_SHORT).show();
                        syncFromService();
                    });
                }

                @Override
                public void onScopeRequestFailed(String message) {
                    ui.post(() -> showScopeFailure("批量加入作用域失败", "（多个应用）", message));
                }
            });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private void removeAll() {
        if (xposed == null) return;
        final java.util.List<String> all = new java.util.ArrayList<>();
        for (Group g : GROUPS) all.add(g.pkg);
        try {
            xposed.removeScope(all);
            for (Group g : GROUPS) Config.uiSet(this, g.key, false);
            Toast.makeText(this, "已移出全部作用域", Toast.LENGTH_SHORT).show();
            syncFromService();
        } catch (Throwable t) {
            Toast.makeText(this, "移除失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** LSPosed 确认后才更新开关与配置 */
    private void commitState(final Group g, final Switch sw, boolean on, String msg) {
        Config.uiSet(this, g.key, on);
        if (sw != null) {
            updatingSwitch = true;
            sw.setChecked(on);
            updatingSwitch = false;
        }
        setGroupUi(g, on);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void execSu(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Throwable t) {
            Logs.i("su failed: " + cmd + " -> " + t);
        }
    }

    /* ---------------- 环境检测 ---------------- */

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /* ---------------- 组状态联动（真实作用域驱动，不置灰） ---------------- */

    /**
     * 把一组的状态刷到界面上。
     * 子项永远可点、永远满透明度；on 只反映这一组真实是否在作用域里。
     */
    /**
     * 作用域请求被拒时给出可操作的说明。
     * 常见于系统 priv-app：LSPosed 的 requestScope 会拒绝，但管理器里手动勾选可以。
     */
    private void showScopeFailure(String title, String pkg, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("包名：" + pkg
                        + "\n\nLSPosed 返回：" + (message == null || message.isEmpty() ? "（无）" : message)
                        + "\n\n可以在 LSPosed 管理器 → 模块 → HyperPGuard → 作用域 里手动勾选该应用。"
                        + "\n手动勾选后回到本界面，开关会跟着变成开启。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private void setGroupUi(Group g, boolean on) {
        updatingSwitch = true;
        Switch gs = groupSwitches.get(g.pkg);
        if (gs != null) gs.setChecked(on);
        updatingSwitch = false;

        List<Switch> children = childSwitches.get(g.pkg);
        if (children != null) {
            for (Switch sw : children) {
                sw.setEnabled(on);
                sw.setAlpha(on ? 1f : 0.4f);
            }
        }
        List<LinearLayout> rows = childRows.get(g.pkg);
        if (rows != null) {
            for (LinearLayout r : rows) r.setAlpha(on ? 1f : 0.4f);
        }
    }

    private java.util.List<String> safeScope() {
        if (xposed == null) return null;
        try {
            java.util.List<String> sc = xposed.getScope();
            android.util.Log.i("HyperPGuard", "getScope -> " + sc);
            return sc;
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "getScope fail", t);
            return null;
        }
    }

    /**
     * 从 LSPosed 读真实作用域，刷新所有组开关与配置。
     * 这是「开关状态跟着框架走」的唯一入口：onServiceBind / onResume 都调它。
     */
    private void syncFromService() {
        java.util.List<String> scope = safeScope();
        for (Group g : GROUPS) {
            boolean on;
            if (scope == null) {
                on = Config.get(this, g.key, true);
            } else {
                on = scope.contains(g.pkg);
                Config.uiSet(this, g.key, on);
            }
            setGroupUi(g, on);
        }
        if (scope != null && scope.isEmpty() && !autoRequested) {
            // 首次安装且框架里还没有任何作用域：只自动请求一次，之后交给用户
            autoRequested = true;
            requestAll();
        }
    }

    /** 子项被打开时，如果该应用还没进作用域，顺手请求加入（不再靠置灰拦人） */
    private void ensureInScope(final Group g) {
        if (xposed == null) {
            Toast.makeText(this, "LSPosed 服务未连接，请确认已在 LSPosed 中启用本模块",
                    Toast.LENGTH_LONG).show();
            return;
        }
        java.util.List<String> cur = safeScope();
        if (cur != null && cur.contains(g.pkg)) return;
        try {
            xposed.requestScope(Collections.singletonList(g.pkg),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(java.util.List<String> approved) {
                            ui.post(() -> {
                                Config.uiSet(MainActivity.this, g.key, true);
                                setGroupUi(g, true);
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            ui.post(() -> showScopeFailure("「" + g.title + "」加入作用域失败",
                                    g.pkg, message));
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private android.view.View bulkButton(final String label, final Runnable action) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(ACCENT);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(8), 0, dp(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv.setOnClickListener(v -> action.run());
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

    private android.view.View groupNote(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextColor(SUB);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
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

        final Switch sw = new Switch(this);
        groupSwitches.put(g.pkg, sw);
        sw.setChecked(Config.get(this, g.key, true));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                if (updatingSwitch) return;
                boolean current = Config.get(MainActivity.this, g.key, true);
                if (checked == current) return;
                // 先回滚，等 LSPosed 确认后再改
                updatingSwitch = true;
                sw.setChecked(!checked);
                updatingSwitch = false;
                if (checked) {
                    requestAdd(g.pkg, g, sw);
                } else {
                    requestRemove(g.pkg, g, sw);
                }
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
        return buildSwitchRow(row, name, desc, key, def, enabled, null);
    }

    /** 带所属组的版本：打开子项时顺手保证该应用在作用域内，不再靠置灰拦人 */
    private Switch buildSwitchRow(LinearLayout row, String name, String desc,
                                  final String key, boolean def, boolean enabled,
                                  final Group group) {
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
        sw.setChecked(Config.get(this, key, def));
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                Config.uiSet(MainActivity.this, key, checked);
                if (checked && group != null) ensureInScope(group);
            }
        });
        row.addView(sw);
        return sw;
    }

    /* ---------------- 行为库状态 ---------------- */

    private static final String FRAUD_DB =
            "/data/misc/user/0/securitycenter/fraud_detection_data.db";
    private static java.util.Map<String, Integer> lastSeq;

    private void addFraudDbRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setClickable(true);

        TextView t1 = new TextView(this);
        t1.setText("行为库状态");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("读安全中心 fraud_detection_data.db 的写入计数，用来判断拦截是否生效");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> showFraudDb());
        parent.addView(row);
    }

    private void showFraudDb() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));

        TextView tv = new TextView(this);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextIsSelectable(true);
        tv.setText("读取中…");
        box.addView(tv);

        Button refresh = new Button(this);
        refresh.setText("刷新");
        refresh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        refresh.setBackground(null);
        refresh.setTextColor(ACCENT);
        refresh.setAllCaps(false);
        box.addView(refresh);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("行为库状态")
                .setView(sv)
                .setNegativeButton("关闭", null)
                .create();

        final Runnable load = () -> new Thread(() -> {
            final String text = readFraudDb();
            ui.post(() -> tv.setText(text));
        }, "hyperpguard-frauddb").start();

        refresh.setOnClickListener(v -> {
            tv.setText("读取中…");
            load.run();
        });
        dialog.show();
        load.run();
    }

    /** 只读计数：复制主库与 -wal 到私有目录，查 sqlite_sequence，不读任何记录内容 */
    private String readFraudDb() {
        String dir = getFilesDir().getAbsolutePath();
        execShell("cp " + FRAUD_DB + " " + dir + "/fd.db 2>/dev/null; "
                + "cp " + FRAUD_DB + "-wal " + dir + "/fd.db-wal 2>/dev/null; "
                + "chmod 644 " + dir + "/fd.db 2>/dev/null; "
                + "chmod 644 " + dir + "/fd.db-wal 2>/dev/null; echo ok");
        java.io.File f = new java.io.File(dir, "fd.db");
        if (!f.exists() || f.length() == 0) {
            return "读不到行为库。\n\n需要 root 授权，且设备上存在 "
                    + "securitycenter/fraud_detection_data.db。";
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            java.util.TreeMap<String, Integer> now = new java.util.TreeMap<>();
            android.database.Cursor c = db.rawQuery("select name, seq from sqlite_sequence", null);
            while (c.moveToNext()) now.put(c.getString(0), c.getInt(1));
            c.close();
            if (now.isEmpty()) return "库里没有写入记录。";
            StringBuilder sb = new StringBuilder();
            int total = 0;
            for (java.util.Map.Entry<String, Integer> e : now.entrySet()) {
                int v = e.getValue();
                total += v;
                sb.append(e.getKey()).append("  ").append(v);
                Integer old = lastSeq == null ? null : lastSeq.get(e.getKey());
                if (old != null && v != old) sb.append("  (+").append(v - old).append(')');
                sb.append('\n');
            }
            lastSeq = now;
            sb.append("\n共 ").append(now.size()).append(" 张表，累计 ").append(total).append(" 次写入");
            sb.append("\n\n这些数字只增不减。刷新后出现 (+N) 说明期间有写入；");
            sb.append("拦截生效时应当保持不变。");
            return sb.toString();
        } catch (Throwable t) {
            return "读取失败：" + t;
        } finally {
            if (db != null) try { db.close(); } catch (Throwable ignored) { }
            new java.io.File(dir, "fd.db-wal").delete();
            new java.io.File(dir, "fd.db-shm").delete();
            f.delete();
        }
    }

    /* ---------------- 桌面图标 ---------------- */

    private static final String LAUNCHER_ALIAS = "io.github.vivibibi30.hyperpguard.Launcher";

    private boolean launcherIconEnabled() {
        try {
            int st = getPackageManager().getComponentEnabledSetting(
                    new android.content.ComponentName(this, LAUNCHER_ALIAS));
            return st != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Throwable t) {
            return true;
        }
    }

    private void setLauncherIcon(boolean on) {
        try {
            getPackageManager().setComponentEnabledSetting(
                    new android.content.ComponentName(this, LAUNCHER_ALIAS),
                    on ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                       : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Throwable t) {
            Toast.makeText(this, "切换失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** 隐藏桌面图标，入口保留在 LSPosed 模块页 */
    private void addHideIconRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText("隐藏桌面图标");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("隐藏后从 LSPosed 模块页进入本界面");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        final Switch sw = new Switch(this);
        sw.setChecked(!launcherIconEnabled());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                setLauncherIcon(!checked);
                boolean now = !launcherIconEnabled();
                if (now != checked) sw.setChecked(now);
                if (checked) {
                    Toast.makeText(MainActivity.this,
                            "图标已隐藏，可从 LSPosed 模块页进入", Toast.LENGTH_LONG).show();
                }
            }
        });
        row.addView(sw);
        parent.addView(row);
    }

    private ViewGroup buttonRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView view = new TextView(this);
        view.setText("查看日志");
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

        TextView diag = new TextView(this);
        diag.setText("自检");
        diag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        diag.setTextColor(ACCENT);
        diag.setGravity(Gravity.CENTER);
        diag.setPadding(0, dp(8), 0, dp(8));
        diag.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        diag.setOnClickListener(v -> showDiagnostics());
        row.addView(diag);

        return row;
    }

    /** 框架连接、真实作用域、开关数量、已注入进程，用来判断「为什么没拦住」 */
    private void showDiagnostics() {
        Toast.makeText(this, "检测中…（需 root）", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("模块版本：").append(Config.VERSION).append('\n');
                sb.append("LSPosed：").append(xposed == null
                        ? "未连接（重开本界面会自动重连；已注入进程仍以下面的日志为准）"
                        : (xposed.getFrameworkName() + " " + xposed.getFrameworkVersion())).append('\n');

                java.util.List<String> scope = safeScope();
                sb.append("作用域：")
                        .append(scope == null ? "读取失败" : scope.size() + " 个").append("\n\n");

                for (Group g : GROUPS) {
                    boolean in = scope != null && scope.contains(g.pkg);
                    sb.append(in ? "[✓] " : "[ ] ").append(g.pkg);
                    if (!isInstalled(g.pkg)) sb.append("（未安装）");
                    sb.append('\n');
                }

                int local = Config.ui(MainActivity.this).getAll().size();
                int remote = 0;
                try {
                    String raw = android.provider.Settings.Global.getString(
                            getContentResolver(), Config.GLOBAL_KEY);
                    if (raw != null && !raw.isEmpty()) {
                        for (String kv : raw.split("\\|")) if (kv.indexOf('=') > 0) remote++;
                    }
                } catch (Throwable ignored) {
                }
                boolean master = Config.get(MainActivity.this, Config.K_MASTER, true);
                sb.append("\n总开关：").append(master ? "已开启" : "已关闭（所有进程都会跳过安装）");
                sb.append("\n本地开关 ").append(local).append(" 项，系统设置 ").append(remote).append(" 项\n");

                sb.append("\nLSPosed 加载记录：\n");
                String lsp = execShell(
                        "f=$(ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -1); "
                                + "[ -n \"$f\" ] && tail -n 4000 \"$f\""
                                + " | grep -oE '\\([a-z0-9_.]+\\)\\[io\\.github\\.vivibibi30\\.hyperpguard'"
                                + " | sed 's/^(//; s/).*//' | sort -u");
                sb.append(lsp.trim().isEmpty() ? "（日志里没有本模块的加载记录）" : lsp.trim());

                sb.append("\n安装失败记录：\n");
                String fails = execShell(
                        "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: (stage|dispatch|net install|privacy install)' | tail -n 40");
                sb.append(fails.trim().isEmpty() ? "（没有）" : fails.trim());

                sb.append("\n最近 attach 日志：\n");
                String attach = execShell(
                        "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: attach' | tail -n 15");
                sb.append(attach.trim().isEmpty()
                        ? "（logcat 缓冲里没有，长时间运行后会被冲掉）"
                        : attach.trim());

                final String body = sb.toString();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        showText("环境自检", body);
                    }
                });
            }
        }).start();
    }

    private String execShell(String cmd) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            p.waitFor();
            return sb.toString();
        } catch (Throwable t) {
            return "执行失败：" + t;
        } finally {
            if (p != null) p.destroy();
        }
    }

    private void restartApp(final String pkg) {
        Toast.makeText(this, "正在重启 " + pkg + "（需 root）", Toast.LENGTH_SHORT).show();
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

    /** 重启作用域内全部应用，让 hook 重新加载 */
    private void restartAll() {
        final List<String> pkgs = new ArrayList<>();
        java.util.List<String> sc = safeScope();
        if (sc != null && !sc.isEmpty()) {
            pkgs.addAll(sc);
        } else {
            for (Group g : GROUPS) pkgs.add(g.pkg);
        }
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "作用域为空", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "正在重启 " + pkgs.size() + " 个应用（需 root）",
                Toast.LENGTH_SHORT).show();
        final StringBuilder cmd = new StringBuilder();
        for (String p : pkgs) cmd.append("am force-stop ").append(p).append("; ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd.toString()});
                    int code = p.waitFor();
                    msg = "已重启 " + pkgs.size() + " 个应用" + (code == 0 ? "" : "（code=" + code + "）");
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
        Toast.makeText(this, "读取中…（需 root）", Toast.LENGTH_SHORT).show();
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
        String out = execShell(
                "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: |blocked by HyperPGuard' | tail -n 600");
        if (out.trim().isEmpty()) {
            return "没有记录。\n\n"
                    + "这里只列模块自身的运行日志（attach、hook 结果）和被拦截的域名访问；"
                    + "没有发生拦截时就是空的，不代表模块没工作。\n\n"
                    + "可能原因：\n"
                    + "1. root 未授权（su 弹窗没点允许）\n"
                    + "2. 模块刚启用，目标应用还没重启\n"
                    + "3. 对应应用的「启用」开关未打开\n"
                    + "4. 旧日志已被 logcat 环形缓冲覆盖\n\n"
                    + "点「自检」可以看框架连接、作用域和已注入进程。";
        }
        return out;
    }

    private void showLog(final String log) {
        String body = "改动任一开关后，需要点该组右上角「重启」或「全部重启」，"
                + "目标应用重新加载规则才会生效。\n\n" + classify(log);
        showText("模块日志（" + log.split("\n").length + " 行）", body);
    }

    /** 每段最多显示的行数 */
    private static final int LOG_SECTION_LIMIT = 20;

    /** 按未拦截 / 已拦截 / 其他分段，避免混在一起看不出结果 */
    private static String classify(String log) {
        java.util.List<String> blocked = new java.util.ArrayList<>();
        java.util.List<String> passed = new java.util.ArrayList<>();
        java.util.List<String> other = new java.util.ArrayList<>();
        for (String line : log.split("\n")) {
            if (line.trim().isEmpty()) continue;
            if (line.contains("blocked by HyperPGuard")
                    || line.contains(" X(host") || line.contains(" X(strict)")) {
                blocked.add(line);
            } else if (line.contains(": ? ")) {
                passed.add(line);
            } else {
                other.add(line);
            }
        }
        if (blocked.isEmpty() && passed.isEmpty()) return log;
        StringBuilder sb = new StringBuilder();
        sb.append(section("未拦截", passed,
                "未拦截指该域名不在当前规则里，不是拦截失败。\n"));
        sb.append(section("已拦截", blocked, null));
        sb.append(section("模块日志", other, null));
        return sb.toString();
    }

    /** 每段只显示最近若干行，附总数与说明 */
    private static String section(String name, java.util.List<String> lines, String note) {
        if (lines.isEmpty()) return "";
        int total = lines.size();
        int from = Math.max(0, total - LOG_SECTION_LIMIT);
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("（共 ").append(total).append(" 条");
        if (total > LOG_SECTION_LIMIT) sb.append("，显示最近 ").append(LOG_SECTION_LIMIT).append(" 条");
        sb.append("）\n");
        if (note != null) sb.append(note);
        for (int i = from; i < total; i++) sb.append(lines.get(i)).append('\n');
        sb.append('\n');
        return sb.toString();
    }

    private void showText(String title, final String log) {
        TextView tv = new TextView(this);
        tv.setText(log);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle(title)
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
        i.putExtra(Intent.EXTRA_SUBJECT, "运行日志");
        i.putExtra(Intent.EXTRA_TEXT, log);
        startActivity(Intent.createChooser(i, "分享运行日志"));
    }

    /* ---------------- 关于 / 更新 ---------------- */

    private ViewGroup linkRow(String label, String value, final String url) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setClickable(true);
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(TEXT);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(name);
        TextView link = new TextView(this);
        link.setText(value);
        link.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        link.setTextColor(ACCENT);
        row.addView(link);
        row.setOnClickListener(v -> openUrl(url));
        return row;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Throwable t) {
            Toast.makeText(this, "没有可打开链接的应用", Toast.LENGTH_SHORT).show();
        }
    }

    /** 只在用户点击时请求一次 GitHub API 取最新 tag，不做后台轮询 */
    private void checkUpdate() {
        Toast.makeText(this, "正在检查更新…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String tag = null, err = null;
            java.net.HttpURLConnection c = null;
            try {
                c = (java.net.HttpURLConnection) new java.net.URL(API_LATEST).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "HyperPGuard/" + Config.VERSION);
                int code = c.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String ln;
                    while ((ln = br.readLine()) != null) sb.append(ln);
                    br.close();
                    tag = new org.json.JSONObject(sb.toString()).optString("tag_name", "");
                } else {
                    err = "HTTP " + code;
                }
            } catch (Throwable t) {
                err = t.getClass().getSimpleName();
            } finally {
                if (c != null) c.disconnect();
            }
            final String fTag = tag, fErr = err;
            ui.post(() -> showUpdateResult(fTag, fErr));
        }, "hyperpguard-check-update").start();
    }

    private void showUpdateResult(String tag, String err) {
        if (err != null) {
            Toast.makeText(this, "检查失败：" + err, Toast.LENGTH_LONG).show();
            return;
        }
        String v = tag == null ? "" : tag.trim().replaceAll("^[vV]", "");
        if (v.length() == 0) {
            Toast.makeText(this, "没拿到版本号，稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (compareVersion(v, Config.VERSION) > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("发现新版本 v" + v)
                    .setMessage("当前 v" + Config.VERSION + "，最新 v" + v + "。")
                    .setPositiveButton("打开发布页", (d, w) -> openUrl(URL_RELEASES))
                    .setNegativeButton("稍后", null)
                    .show();
        } else {
            Toast.makeText(this, "已是最新版本（v" + Config.VERSION + "）",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static int compareVersion(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int xi = i < x.length ? versionPart(x[i]) : 0;
            int yi = i < y.length ? versionPart(y[i]) : 0;
            if (xi != yi) return xi > yi ? 1 : -1;
        }
        return 0;
    }

    private static int versionPart(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (Throwable t) {
            return 0;
        }
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
