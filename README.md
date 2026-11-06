# PrivacyGuard

面向 MIUI / HyperOS 的隐私上报拦截模块（LSPosed / Xposed）。

[![Build APK](https://github.com/vivibibi30/pguard/actions/workflows/build.yml/badge.svg)](https://github.com/vivibibi30/pguard/actions/workflows/build.yml)

预装系统组件会持续采集设备标识、应用使用记录、位置、通话行为等信息，并通过网络或跨进程接口提供给厂商、运营商或第三方。本模块在**采集入口、本地存储、对外接口、网络出口**四个环节同时设防，按应用分组独立开关。

> 仅供个人在自有设备上学习研究使用。使用前请阅读 [DISCLAIMER.txt](DISCLAIMER.txt)。

---

## 功能

### 网络层

在 `java.net.URL#openConnection`、`InetAddress#getByName/getAllByName`、`Socket` 构造与 `connect` 四个位置拦截，同时覆盖 `HttpURLConnection` 与 OkHttp 两类客户端。规则形式为「域名后缀 +（可选）精确路径前缀」，命中即抛异常，连接不会建立。

### XSOF 行为拦截

小米安全中心的 XSOF 提供「某个时间窗口内是否发生过截图 / 屏幕共享 / 通话」等查询能力，第三方应用认证后即可调用。本模块在四条通道上同时封堵：

| 层 | 目标 | 处理 |
|---|---|---|
| Provider 查询 | `XSOFProvider.query()` | 返回空 Cursor |
| Provider 调用 | `XSOFProvider.call()` | 返回空 Bundle |
| Provider 写入 | `XSOFProvider.insert()` | 返回 null |
| AIDL 接口 | `MiSafetyDetectImpl` 的 7 个方法 | 回调 `(null, 1017)` |
| 本地写库 | `Ra.k` 的 27 个写入方法 | 直接返回，不落盘 |
| 风险评分 | `Pa.b` 评分方法 | 恒返回 0 |

结果是：第三方应用查不到行为记录，本地也不产生新记录。

### 隐私 API 层

不只拦网络出口，直接在数据来源处截断（框架公开签名，与组件版本无关）：

| 目标 | 处理 | 作用范围 |
|---|---|---|
| `ApplicationPackageManager#getInstalledPackages/getInstalledApplications` | 返回空列表 | 小米统计、系统安全组件 |
| `UsageStatsManager#queryEvents/queryUsageStats` | 返回 `null` / 空列表 | 小米统计 |
| `TelephonyManager#getDeviceId/getImei/getMeid/getSubscriberId/getSimSerialNumber` | 返回 `null` | 小米统计、系统安全组件、安全中心、SystemHelper、运营商服务 |
| `SmsManager#sendTextMessage/sendMultipartTextMessage/sendDataMessage` | 不发送 | 运营商服务（短信注册兜底） |
| `Context#sendBroadcast`（action = `com.xiaomi.xmsf.push.XMSF_UPLOAD_ACTIVE`） | 丢弃 | 小米统计交给 XMSF 代发的材料 |

### 方法层

| 目标 | Hook 点 | 效果 |
|---|---|---|
| SystemHelper | `c.d.a.b.f#a(String, byte[], byte[])` | 发送函数返回 false |
| SystemHelper | `c.d.a.b.e#a(String)` | 不再写入待发队列 |
| trustservice | `ServiceManager.getService/waitForService` 含 `mrm` | MRM 服务取不到，远程认证链断开 |
| analytics | `d.f.n#y(long,long)`（6.19.0 实测） | 应用使用记录不生成 |
| analytics | `onetrack.p.u#w` / `c.f.k#y` / `onetrack.q.c#a,b,c`（旧版） | 存在才挂，版本不符仅记 `hook miss` |
| analytics | 扫描所有 `(long, long) -> JSONObject` 方法（6.33.0 起包名变成 `a.b.a.*`） | 命中即置空，日志输出 `analytics scan scanned=N hooked=N` |

方法层与网络层互不依赖：类名变化时域名规则仍在工作，域名更换时方法层仍在工作，日志中可分辨是哪一层在起作用。

### 广告

应用商店的广告配置下发、服务框架的广告接口、安装器的广告位配置一并拦截。

---

## 界面预览

| 总开关 / 需要 root 的功能 | 分组卡片与子项 |
|---|---|
| ![主界面](docs/images/ui-main.png) | ![分组卡片](docs/images/ui-cards.png) |

每组右上角的开关就是该应用在 LSPosed 里的作用域开关；组关闭时，组内子项置灰且不可操作。

---

## 覆盖组件

| 组件 | 包名 | 拦截内容 | 默认 |
|---|---|---|---|
| SystemHelper | `com.mobiletools.systemhelper` | 运营商遥测：设备 / SIM / 基站 / 应用列表 | 开 |
| 系统安全组件 | `com.miui.guardprovider` | 应用列表云查、AI 通话预警云检测、反诈配置拉取；Flamingo 云查上传默认关闭 | 开 |
| 安全中心 | `com.miui.securitycenter` | XSOF 查询与写库、反诈配置拉取、志愿者传感器上报；云查 / 合作方鉴权 / 月报 / 防窥分类 / 地震报警定位 / ADB 验证默认关闭 | 开 |
| 小米统计 | `com.miui.analytics` | tracking / om.tracking / sdkconfig 整域，含 DoH 旁路 | 开 |
| 应用安装器 | `com.miui.packageinstaller` | AVL 上报、活跃统计、安装页广告位 | 开 |
| 应用商店 | `com.xiaomi.market` | 广告配置下发 | 开 |
| 小米服务框架 | `com.xiaomi.xmsf` | 广告接口 | 开 |
| 小米可信服务 | `com.xiaomi.trustservice` | MRM 远程认证链（关）、设备证书/密钥上报（关） | 关 |
| 翻译组件 | `com.xiaomi.aiasst.vision` | 语音识别上传（`/speech/`） | 关 |
| 运营商服务 | `com.xiaomi.registration` | 设备注册（zzhc，关）、明文 CTEI 上报（开） | — |

默认关闭的两项（小米可信服务、翻译组件）会影响设备证书、查找设备与语音识别，需要时再单独开启。

---

## 已验证环境

| 项目 | 环境 |
|---|---|
| 系统 | Android 16 / HyperOS |
| 框架 | LSPosed 2.2.1（libxposed API 102） |
| 安全中心 | 12.8.6 |
| 系统安全组件 | 2.5.6 |
| SystemHelper | 3.0.1 |
| 应用安装器 | 正常拦截 `/avl/upload`、`/info/layout` |

其他机型与系统版本需自行检查日志中的 `hook miss` 与抓取模式记录。本模块不按版本号限制加载，类名或签名变化时方法层会自动跳过，网络层不受影响。

---

## 环境要求

- Android 10 及以上（minSdk 29）
- Root：KernelSU / Magisk / APatch
- LSPosed（支持 libxposed API 100+，实测 2.2.1）

本模块不使用 DexKit 或任何 DEX 扫描，APK 约 49 KB（含 libxposed service 与 AIDL），进程启动时开销可忽略。

---

## 安装

1. 安装 `PrivacyGuard-1.1.0.apk`
2. 打开 LSPosed → 模块，启用「隐私上报拦截」
3. 首次打开模块会自动向 LSPosed 请求全部作用域，允许即可；也可在 LSPosed 里手动勾选：

   ```
   com.mobiletools.systemhelper    com.miui.guardprovider
   com.miui.securitycenter         com.miui.analytics
   com.miui.packageinstaller       com.xiaomi.market
   com.xiaomi.xmsf                 com.xiaomi.trustservice
   com.xiaomi.aiasst.vision
   ```

4. 在模块里对目标应用点一次「重启」，让它重新加载即可生效

---

## 使用

### 分组开关

每个应用一张卡片，右上角是**该组总开关**。关掉后这个应用完全不注入，不产生任何 hook。

卡片内部是子项开关，可以只关某一条规则。例如只想要 XSOF 拦截、不要域名拦截，把「反诈配置拉取」关掉即可。

### 作用域联动

每个应用卡片右上角的「启用」开关**直接控制 LSPosed 作用域**，走的是 libxposed 官方接口：

| 动作 | 接口 |
|---|---|
| 读当前作用域 | `XposedService#getScope()` |
| 加入作用域 | `XposedService#requestScope(list, callback)` |
| 移出作用域 | `XposedService#removeScope(list)` |

界面上的组开关状态不是本地默认值，而是每次绑定服务、每次回到界面时用 `getScope()` 回填的真实结果；
切换时先回弹，等 `onScopeRequestApproved` 回调成功才真正落地。
应用总开关关闭时，其下子项变灰且不可操作；重新打开后恢复。

旧版本采用的「直接改 `modules_config.db`」已废弃：LSPosed 有内存缓存，改库不但不刷新界面，
还容易被覆盖回旧值。

### 重启

改完开关点卡片上的「重启」，模块会执行 `am force-stop <包名>`。应用下次启动时重新注入，新配置才会生效。

系统常驻组件（安全中心、服务框架）被停止后通常几秒内会被系统重新拉起，这正好等于自动完成重启。

### 抓取模式

打开后，**没有被拦截**的访问也会记录一条日志（同一域名 60 秒内只记一次）。

用途：发现漏网的上报域名。跑一段时间 → 关掉 → 查看记录，日志里带 `?` 的条目就是「访问了但没被规则命中」的目标。

平时保持关闭。

### 日志

「查看记录」读取系统 logcat 中本模块的输出，需要 root 授权。

「分享日志」直接拉起系统分享，可发到微信 / QQ / 邮件。

---

## 日志说明

| 前缀 | 含义 |
|---|---|
| `attach <包名>` | 模块注入某进程成功 |
| `X(host)` | 域名规则命中 |
| `X(host+path)` | 域名 + 路径规则命中 |
| `X(dns)` | DNS 解析层命中 |
| `? <tag> <host>` | 抓取模式记录：访问未被拦截 |
| `hook ok` | 方法层 hook 安装成功 |
| `hook miss` | 方法层类名或签名不匹配（域名层仍在工作） |
| `xsof provider hooked` | XSOF Provider 三个入口已拦截 |
| `xsof aidl hooked=N` | XSOF AIDL 接口拦截数量 |
| `xsof db write hooked=N` | XSOF 本地写库方法拦截数量 |
| `xsof score hooked=N` | 风险评分方法拦截数量 |

日志不输出电话号码、行为数据库内容或调用参数。

---

## 如何确认生效

1. 启用模块，并对目标应用点一次「重启」
2. 打开任意一个受保护的应用，触发一次它的上报行为（例如打开安全中心、安装一个应用）
3. 在模块里点「查看记录」，应看到：

   ```
   [PrivacyGuard] attach guardprovider, rules=3
   [PrivacyGuard] guardprovider X(host) flash.sec.miui.com
   ```

4. 检查 XSOF 部分是否全部为 `hooked`，没有 `miss`：

   ```
   [PrivacyGuard] xsof provider hooked
   [PrivacyGuard] xsof aidl hooked=7
   [PrivacyGuard] xsof db write hooked=27
   [PrivacyGuard] xsof score hooked=1
   ```

5. 若某个应用日志里只有 `attach` 没有 `X(...)`，说明规则没命中——开抓取模式跑一段时间，把 `?` 开头的域名补进规则。

---

## 构建

需要 JDK 11+、build-tools 35 及以上（`d8` / `aapt2` / `zipalign` / `apksigner`），以及一份 `android.jar`。

```sh
export ANDROID_JAR=/path/to/platforms/android-35/android.jar
export D8=/path/to/build-tools/35.0.0/d8
export AAPT2=/path/to/build-tools/35.0.0/aapt2
export ZIPALIGN=/path/to/build-tools/35.0.0/zipalign
export APKSIGNER=/path/to/build-tools/35.0.0/apksigner
sh build.sh
```

打包顺序：`javac → d8 → aapt2 link → 追加 classes.dex → 追加 META-INF → zipalign → apksigner`。

`aapt2 link` 需要一份带资源表的 jar。CI 用完整平台包；本地如果 `android.jar` 资源表被裁剪过，
放一份 `android-stub.jar` 在仓库根目录，脚本会自动优先使用它（也可以用 `AAPT2_INCLUDE` 指定）。

CI：`.github/workflows/build.yml` 在每次推送到 `main` 时自动构建，产物以 artifact 形式提供。

---

## 源码结构

| 文件 | 职责 |
|---|---|
| `HookEntry.java` | 入口，继承 `XposedModule`，在 `onPackageReady` 按包名分发 |
| `Config.java` | 开关常量；hook 侧当前按默认值执行（细粒度开关待接 RemotePreferences） |
| `MainActivity.java` | 分组开关面板、作用域联动、重启、日志查看与分享 |
| `assets/DISCLAIMER.txt` | 免责声明，应用内可查看 |

```
src/io/github/vivibibi30/pguard/   源码
res/values/                        app_name 等字符串资源
assets/                            免责声明
META-INF/xposed/                   模块声明（必须打到 APK 根）
libs/libxposed-api.jar             libxposed API（compileOnly，不打进 dex）
libs/libxposed-service.jar         service + interface 合并版（必须打进 dex）
libs/api-82.jar                    传统 Xposed API（编译兼容用）
docs/                              技术笔记与规则说明
build.sh                           一键构建
```

---

## 适配边界

- 拦截范围限于已经识别的路径与规则，不代表覆盖 HyperOS 的全部采集或联网行为
- 方法层依赖当前版本的类名与签名，系统组件升级后可能 `hook miss`；此时网络层仍然工作
- 使用 native 网络栈（如 Cronet）的请求不受 Java 层 hook 影响
- analytics 会把材料通过 XMSF 广播交出去，这一段属于跨进程交接，只拦 analytics 自身 UID 覆盖不到
- 若某个域名同时承担上报与功能，本模块按路径拆分处理；未拆分的条目默认关闭
- 本地数据库不再增长不能单独证明不存在其他采集或上传途径
- 模块不修改系统 APK，不删除已有数据，停用后行为立即恢复

---

## 问题反馈

提交 Issue 时请提供：

- 机型与完整系统版本
- 目标组件的版本号
- 模块版本
- 相关日志（可在应用内「分享日志」导出，注意脱敏）

如涉及某次操作，请说明复现步骤、预期结果、实际结果，以及停用模块后是否能恢复。

---

## 参考来源

本项目的拦截目标与识别方式查阅了以下项目的公开资料。**全部代码为独立编写，未复制、未反编译复用上述项目的实现。**

### HyperCeiler · GPL-3.0

<https://github.com/Sevtinge/HyperCeiler> · 作者 Sevtinge

| 参考点 | 本项目对应实现 |
|---|---|
| `rules/analytics/FuckMiuiUpload` 的 analytics 目标方法 | 采用思路但不用固定类名：旧名保留做兼容，实际以 `(long,long) -> JSONObject` 签名扫描覆盖各版本 |
| `rules/guardprovider/DisableUploadAppListNew` 指向的 `flash.sec.miui.com/detect/app` | guardprovider 域名规则 |
| `rules/packageinstaller/DisableAppInfoUpload` 涉及的 `/avl/upload`、`/info/layout` 路径 | 安装器路径规则（`/interceptcheck` 判断为功能接口，未采纳） |
| `rules/trustservice/DisableMrm` 的 MRM 拦截思路 | 改在 `ServiceManager` 层屏蔽，不依赖混淆类名 |

### XSOF Privacy · 未声明许可

<https://modules.lsposed.org/module/io.github.yunyizea.xsofprivacy> · 作者 yunyizea

| 参考点 | 本项目对应实现 |
|---|---|
| 8 类行为记录（`airplane_mode` / `call_forwarding` / `fraud_phone` / `fraud_url` / `install_app` / `screen_share` / `screenshot` / `voip_record`）的拦截范围 | `Ra.k` 的 27 个写入方法 hook |
| 对外查询拒绝语义 `(null, 1017)`、不返回伪造的“零风险”评分 | AIDL 回调 `G2(null, 1017)` |
| 按功能分组组织 hook、逐组报告状态的思路 | 四层结构 + `xsof ... hooked=N` 日志 |
| 仅作用于目标进程、不修改系统 APK、不删除已有数据的边界 | 同 |

### Hyper 隐私卫士 · 未声明许可

包名 `io.github.hyperprivacyguard`，未附带项目地址或作者信息，仅在 APK 内分发 DexKit 与 Kotlin 的第三方许可证。

| 参考点 | 本项目对应实现 |
|---|---|
| SystemHelper 采集入口特征（`getCurDeviceInfo`、`listenNetworkState`、`HistoryPostThread`） | 未实现；本机 SystemHelper 已停用，仅参考其识别方式 |
| guardprovider 应用列表与 APK 特征采集入口 | 未实现具体方法 hook，由 `flash.sec.miui.com` 域名层覆盖 |
| `AiGuardService` 生命周期拦截思路 | 未采纳（该功能可在系统设置中关闭） |

### HyperOS4-XSOF-Analysis · 作者 FAQQAQ

<https://github.com/FAQQAQ/HyperOS4-XSOF-Analysis>

7 个 HyperOS 组件的静态逆向记录，本版大量拦截范围来自这份报告，特别感谢作者把证据强度（确认发送实现 / 保留实现 / 未闭环）写清楚。

| 参考点 | 本项目对应实现 |
|---|---|
| `om.tracking.miui.com/track/mt`、区域与备份域（`tracking.india/rus/eu/intl`、`pn-*-bak.onetrack.mi.com`） | 「小米统计」整域规则（含 `onetrack.mi.com`） |
| 6.33.0 国际版 DoH + IP 缓存重试（`dns.google`、bootstrap `8.8.4.4`/`8.8.8.8`） | 「阻断 DoH 旁路」子项 |
| analytics 6.33.0 的 `a.b.a.*` 结构与使用记录采集 | 签名扫描方法层（不硬编码类名） |
| `framework.sec.miui.com`（XSOF 合作方鉴权）、`virus-engine.sec.miui.com`（云查/风险配置）、`data.sec.miui.com`（月报） | 安全中心 / 系统安全组件中默认关闭的子项 |
| 地震/志愿者/报警/EPS 族（`api.sec.miui.com`、`srv.sec.miui.com`、`gateway-papa`、`eps.caict.ac.cn`） | 「地震/报警/EPS 定位」与「志愿者传感器上报」子项 |
| `flash.sec.miui.com/user/cat`（防窥应用分类上传包名数组） | 「防窥分类上报」子项 |
| `com.xiaomi.registration`：`zzhc.vnet.cn`、`typdm.tydevice.com` 明文 CTEI、`sms:10659401` | 新增运营商服务分组，短信从 `SmsManager` 侧拦 |
| analytics 交 XMSF 代发（`XMSFUploadReceiver`） | `Context#sendBroadcast` 按 action 丢弃 |
| `find.api.micloud.xiaomi.net`（设备证书/密钥） | MiTrustService 的「设备证书/密钥上报」子项 |

### 第三方组件

| 组件 | 许可 | 用途 |
|---|---|---|
| [Xposed API 82](https://api.xposed.info/) | Apache-2.0 | 编译期依赖，`libs/api-82.jar`，不打包进模块 |
| [libxposed API 102](https://github.com/libxposed/api) | Apache-2.0 | 模块入口与作用域接口，`libs/libxposed-api.jar`（compileOnly）+ `libs/libxposed-service.jar`（打包） |

本项目不包含 DexKit、dexlib2 或任何 DEX 解析库，因此 APK 体积保持在约 49 KB。

## 更新日志

### 1.1.0

- 新增**隐私 API 层**：应用列表、使用记录、设备标识、短信发送、XMSF 代发广播，全部在数据来源处截断
- 「小米统计」改为整域拦截（`miui.com` / `xiaomi.com` / `xiaomi.net` / `onetrack.mi.com` / `pubsub.googleapis.com`），并新增「阻断 DoH 旁路」（`dns.google`、`8.8.8.8` / `8.8.4.4`）
- analytics 方法层：`d.f.n#y(long,long)`（6.19.0 实测）+ `(long,long) -> JSONObject` 签名扫描，覆盖 6.33.0 的 `a.b.a.*` 改名
- 新增**运营商服务**分组：`typdm.tydevice.com` 明文 HTTP 上传 IMEI/MAC 默认拦，`zzhc.vnet.cn` 设备注册默认关
- 安全中心新增子项：防窥分类上报、地震/报警/EPS 定位、ADB 授权验证、Flamingo 云查上传、设备证书/密钥上报（均默认关闭）；志愿者传感器上报默认拦
- 修复：组门 `Config.on(key, false)` 恒返回 fallback，导致 SystemHelper / 安全中心 / MiTrustService / 安装器 / 商店 / XMSF 六段 hook 从未安装
- 版本号 1.1.0 / versionCode 3

### 1.0.1

- 迁移到 libxposed 新 API（`XposedModule` / `onPackageReady`），模块 API 102
- 作用域改为官方接口：`getScope()` 读、`requestScope()` / `removeScope()` 写，弃用改 `modules_config.db`
- 组开关状态实时取自 LSPosed 真实作用域，绑定服务与回到界面各同步一次
- 应用总开关关闭时，其下子项置灰且不可操作
- 「重启」「查看记录」的 root 提示写进设置卡片，启动不再自动 `su`
- 构建：固定签名 `debug.keystore` 并校验指纹；`META-INF` 打进 APK 根目录；CI 使用 build-tools 35
- 移除「服务与反馈」（`com.miui.miservice`）占位

### 1.0.0

- 首个版本：四环节拦截、按应用分组、传统 Xposed API 82

---

## 免责声明

- 本模块仅用于在**本人拥有完全控制权**的 Android 设备上限制预装组件的上报行为，不提供任何入侵、破解他人系统或获取他人数据的能力
- 不做商业销售、付费分发，不得移除声明后再次分发
- 模块自身不联网、不收集、不上传任何信息；开关配置存在本地，日志只在本地 logcat 读取
- 可能影响依赖同一接口的系统功能（设备证书、查找设备、病毒库更新、语音识别等），默认关闭的项目需自行评估后开启
- 按「现状」提供，不提供任何明示或暗示担保；因使用或无法使用本模块造成的损失由使用者自行承担

完整八条条款见 [DISCLAIMER.txt](DISCLAIMER.txt)。

---

## 许可

GPL-3.0，见 [LICENSE](LICENSE)。

使用前请阅读 [DISCLAIMER.txt](DISCLAIMER.txt)。

---

### English

Xposed module for MIUI / HyperOS that blocks data collection and uploads from
preinstalled system components: carrier telemetry, cloud app scanning,
installer stats, ads, and XSOF behaviour queries.

Interception happens at four stages — collection entry, local storage, IPC
interfaces, and network egress — with per-app switches. Requires root + LSPosed.
No DEX scanning; ~49 KB. Requires libxposed API 100+ (tested on LSPosed 2.2.1).

Personal use only. See [DISCLAIMER.txt](DISCLAIMER.txt).
