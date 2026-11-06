# 技术笔记

本文件记录拦截点的来源与验证方式。结论均来自对目标 APK 的静态分析，以及在 Android 16 / HyperOS 设备上的运行时观察。

## SystemHelper · `com.mobiletools.systemhelper`

- 位于 `/system/priv-app`，带私享权限，配套 `privapp-permissions-systemhelper.xml` 中标注为运营商定制（`CUST_CmctDm`）
- **真正的总开关**是 `Settings.Secure.operator_service_improve_plan == 1`，而不是"是否使用某运营商卡"。代码中对 46001/46006/46009/46010 的判定，只用于"无 Wi-Fi 时是否仍发送"
- 开机后延迟 240 秒首次上报（`OnBootReceiver` 中 `240000L`）
- 使用 4 个精确闹钟：`GetSignalInfoReceiver` / `AppInfoRegisterReceiver` / `ScheduleRegisterReceiver` / `SignalInfoRegisterReceiver`
- 上报地址硬编码为明文 HTTP：`http://dm.wo.com.cn:18080/register`
- 请求体经 AES 加密并附 `sign` 参数，`Content-Type: application/encrypted-json`
- 本地库 `dmregister` 滚动保留最近 10 条，字段含 IMEI（明文）、ICCID/IMSI（部分打码）、TAC/CID（哈希）、内存/存储、IPv6、应用列表（包名 / 前台分钟数 / 流量）
- 位置数据经内置百度定位 SDK 获取
- `RegisterHelperService` 为 `exported=true` 且未声明 `android:permission`，任意应用可绑定并改写上报地址

> 因此本模块除了拦域名，还对 `c.d.a.b.f#a(String, byte[], byte[])`（唯一发送出口）与 `c.d.a.b.e#a(String)`（写库）加了方法层拦截。

## 系统安全组件 · `com.miui.guardprovider`

- `AiGuardService` 声明 `foregroundServiceType="mediaProjection|microphone"`
- 内部字段与统计维度：`phoneVoiceDetectionTimes` / `qqVoiceDetectionTimes` / `wechatVoiceDetectionTimes` / `qqVideoDetectionTimes` / `wechatVideoDetectionTimes`
- 音频识别链路：

  ```
  AiGuardService（采集）
    → p098o1/b.postData(pcm[], false, dialogId)
    → AiCapability.startRecognize("zh-CN", "zh-CN", ...)
    → AiCore.PKG_AI_ASST_VISION = "com.xiaomi.aiasst.vision"
      AiCore.ACTION_TRANSLATE_SDK_SERVICE =
        "com.xiaomi.aiasst.vision.ACTION_TRANSLATE_SDK_SERVICE"
    → 跨进程 AIDL 到翻译组件
    → speech*.xiaomixiaoai.com/speech/...
    → 识别文本回传 guardprovider
    → detection-aigc.engine.miui.com（每 10 秒云检测）
  ```

- 该 APK 中 `sec.miui.com` 的子域共有三个：`flash.sec.miui.com`（应用特征上传）、`virus-engine.sec.miui.com`（**病毒库下载，不可拦**）、`api.sec.miui.com`
- 因此规则只写到 `flash.sec.miui.com`，不使用 `sec.miui.com` 通配

## 小米可信服务 · `com.xiaomi.trustservice`

- `MiTrustService` 通过 `vendor.xiaomi.hardware.mrm.IMrm` 做本地证书链校验，字段包括 `unlock_l1` / `unlock_l2` / `root_l1` / `selinux_l1` / `fr_counter_l1`
- 可用性判断位于 `R0.h#b()`：内部通过 `ServiceManager` 取 `vendor.xiaomi.hardware.mrm.IMrm/default`
- 本模块不 hook 该混淆类，而是在 `ServiceManager.getService/waitForService` 层拦截名称含 `mrm` 的请求，避免随版本变更失效
- 另有 `find.api.micloud.xiaomi.net` 用于设备证书（`PubCertRevise`），拦截会影响"查找设备"，故不纳入默认规则

## 翻译组件 · `com.xiaomi.aiasst.vision`

- 应用名（`app_name_cn`）为「翻译」，无桌面入口
- 主要 Activity：`TextTranslateEditActivity`、`TextTranslateMicActivity`、`SimultaneousInterpretationActivity`、`AITranslateDetailsActivity`
- 域名以 `*.xiaomixiaoai.com` 为主，识别接口路径为 `/speech/v1.0/...`、`/speech/v2.0/...`
- 代码内自带 onetrack 埋点，但无独立上报域名；空闲状态下短时抓包未见自发请求
- 真正的语音服务是另一个包 `com.xiaomi.aiasst.service`
- 规则因此只针对 `/speech/` 路径，避免连带切断文字翻译

## 安全中心 · `com.miui.securitycenter`

- XSOF 实现在此：`com.xiaomi.security.xsof.XSOFProvider`（`exported=true`，未声明 `android:permission`）
- `com.xiaomi.security.permission.ACCESS_XSOF` 的 `protectionLevel="normal"`
- `FraudDetectionProvider` 另需 signature 级 `ACCESS_XSOF_FRAUD_DETECTION`
- 结果键：`xsof_fraud_detection_{current,past,duration,score}_result`
- `fraud_detection_data.db` 的路径字符串存在于代码中，但是否生成取决于运行状态

## 应用安装器 · `com.miui.packageinstaller`

| 接口 | 用途 | 是否拦截 |
|---|---|---|
| `/avl/upload` | 上传 | 拦 |
| `/statistics/appactive` | 活跃统计埋点 | 拦 |
| `/info/layout` | 返回 `AdModel`（广告位配置） | 拦（归入广告组） |
| `/interceptcheck` | 游戏安装安全拦截（功能） | **不拦** |

## 小米统计 · `com.miui.analytics`（分组已移除）

原实现按公开资料硬编码了 5 个方法：`onetrack.p.u#w(long,long)`、`c.f.k#y(long,long)`、
`onetrack.q.c#a(Context)/#b(Context)/#c()`。对原始 APK（6.19.0，1.4 MB）反编译核对后发现全部对不上：

| 目标 | 6.19.0 实际情况 |
|---|---|
| `com.miui.analytics.c.f.k` | 类不存在 |
| `com.miui.analytics.onetrack.p.u#w` | 类在，无 `w` 方法（实际为 `s`/`t`/`v`/`a`/`u`） |
| `com.miui.analytics.onetrack.q.c#a/#b/#c` | 类在，签名是 `(String,long)`、`(boolean,String,IOException,String)`、`(String,int,String)` |

真实位置是 `com.miui.analytics.d.f.n#y(long,long)`（返回应用使用记录的 JSONObject）和
`com.miui.analytics.onetrack.q.c` 的 `f`/`g`/`h`/`i`/`j`/`k`/`l`/`m`（自带 HttpURLConnection 发送）。

由于该类名随版本变化、本机又用模块把 AnalyticsCore 替换成了空壳，1.0.1 起直接移除「小米统计」分组，
不再提供无效开关。


## 观察到的历史失效点

| 现象 | 原因 | 处理 |
|---|---|---|
| 只拦 `URL.openConnection` 时 OkHttp 请求照常发出 | OkHttp 走 Socket 直连 | 增加 `InetAddress` 与 `Socket.connect` 层 |
| 病毒库更新失败 | 规则写了 `sec.miui.com` 通配 | 收窄为 `flash.sec.miui.com` |
| 安装流程异常 | 拦了 `/interceptcheck` | 移除该路径 |
| 方法层静默失效 | 混淆类名随版本变化 | 保留域名层作兜底，并输出 `hook miss` 日志 |

## libxposed 新 API 迁移结论（1.0.1）

真机实测环境：HyperOS / Android 16，LSPosed 2.2.1，模块 API 102。

| 现象 | 结论 |
|---|---|
| LSPosed 判定「不是模块」，日志无任何记录 | `META-INF/xposed/*` 被打进了 `assets/META-INF/`；必须放在 APK 根目录 |
| 启动即 `NoClassDefFoundError: IXposedService$Stub` | `io.github.libxposed:service` 不含 AIDL 产物，需从 `:interface` 的 classes.jar 合并后再 d8 |
| 组开关状态与实际不符 | `getScope()` 读取真实作用域后回填；本地 SharedPreferences 只做兜底 |
| 直接改 `modules_config.db` 后界面不变 | LSPosed 有内存缓存，改库不刷新且会被覆盖，改用 `requestScope()` / `removeScope()` |
| `staticScope=true` 时能否动态改作用域 | 可以。`requestScope()` 实测可把作用域从 3 个扩到 8 个，`getScope()` 立即反映 |
| 覆盖安装后 KernelSU 授权失效 | 卸载重装会重新分配 uid，而 KernelSU 按 uid 记录授权；保持固定签名 + `pm install -r` 即可保留 |
| `pm install` 读 `/sdcard` 报 `Can't open file` | `system_server` 无 fuse 读权限，先 `cp` 到 `/data/local/tmp` 再装 |
