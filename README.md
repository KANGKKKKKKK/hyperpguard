# PrivacyGuard

面向 MIUI / HyperOS 的隐私上报拦截模块（LSPosed / Xposed）。

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

### 方法层

| 目标 | Hook 点 | 效果 |
|---|---|---|
| SystemHelper | `c.d.a.b.f#a(String, byte[], byte[])` | 发送函数返回 false |
| SystemHelper | `c.d.a.b.e#a(String)` | 不再写入待发队列 |
| trustservice | `ServiceManager.getService/waitForService` 含 `mrm` | MRM 服务取不到，远程认证链断开 |
| analytics | `onetrack.p.u#w` / `c.f.k#y` | 返回空 |
| analytics | `onetrack.q.c#a/#b/#c` | 返回 `0` / `"NONE"` / `false` |

方法层与网络层互不依赖：类名变化时域名规则仍在工作，域名更换时方法层仍在工作，日志中可分辨是哪一层在起作用。

### 广告

应用商店的广告配置下发、服务框架的广告接口、安装器的广告位配置一并拦截。

---

## 覆盖组件

| 组件 | 包名 | 拦截内容 | 默认 |
|---|---|---|---|
| SystemHelper | `com.mobiletools.systemhelper` | 运营商遥测：设备 / SIM / 基站 / 应用列表 | 开 |
| 系统安全组件 | `com.miui.guardprovider` | 应用列表云查、AI 通话预警云检测、反诈配置拉取 | 开 |
| 安全中心 | `com.miui.securitycenter` | XSOF 查询与写库、反诈配置拉取 | 开 |
| 小米统计 | `com.miui.analytics` | onetrack / mistat 上报 | 开 |
| 应用安装器 | `com.miui.packageinstaller` | AVL 上报、活跃统计、安装页广告位 | 开 |
| 应用商店 | `com.xiaomi.market` | 广告配置下发 | 开 |
| 小米服务框架 | `com.xiaomi.xmsf` | 广告接口 | 开 |
| 小米可信服务 | `com.xiaomi.trustservice` | MRM 远程认证链 | 关 |
| 翻译组件 | `com.xiaomi.aiasst.vision` | 语音识别上传（`/speech/`） | 关 |

默认关闭的三项会影响设备证书、查找设备、翻译与语音识别功能，需要时再单独开启。

---

## 已验证环境

| 项目 | 环境 |
|---|---|
| 系统 | Android 16 / HyperOS |
| 框架 | LSPosed（传统 Xposed API 82） |
| 安全中心 | 12.8.6 |
| 系统安全组件 | 2.5.6 |
| SystemHelper | 3.0.1 |
| 应用安装器 | 通过 |

其他机型与系统版本需自行检查日志中的 `hook miss` 与抓取模式记录。本模块不按版本号限制加载，类名或签名变化时方法层会自动跳过，网络层不受影响。

---

## 环境要求

- Android 10 及以上（minSdk 29）
- Root：KernelSU / Magisk / APatch
- LSPosed（传统 Xposed API 82）

本模块不使用 DexKit 或任何 DEX 扫描，体积约 30 KB，进程启动时开销可忽略。

---

## 安装

1. 安装 `PrivacyGuard-1.0.0.apk`
2. 打开 LSPosed → 模块，启用「隐私上报拦截」
3. 勾选作用域（安装时会自动推荐）：

   ```
   com.mobiletools.systemhelper    com.miui.guardprovider
   com.miui.securitycenter         com.miui.analytics
   com.miui.packageinstaller       com.xiaomi.market
   com.xiaomi.xmsf                 com.xiaomi.trustservice
   com.xiaomi.aiasst.vision
   ```

4. 重启手机，或在模块里对每个应用点一次「重启」

没勾选作用域的应用不会注入，等于没装。

---

## 使用

### 分组开关

每个应用一张卡片，右上角是**该组总开关**。关掉后这个应用完全不注入，不产生任何 hook。

卡片内部是子项开关，可以只关某一条规则。例如只想要 XSOF 拦截、不要域名拦截，把「反诈配置拉取」关掉即可。

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
| `X(strict)` | 严格模式，该进程全部外网被断 |
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

1. 启用模块并重启手机
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

需要 JDK 11+、`android.jar`、`d8`、`aapt2`、`zipalign`、`apksigner`。

```sh
export ANDROID_JAR=/path/to/android.jar     # 平台 android.jar
export D8=/path/to/d8                        # build-tools 中的 d8
export AAPT2=/usr/bin/aapt2
export KEYSTORE=/path/to/debug.keystore      # 缺省用 ./debug.keystore
sh build.sh
```

`aapt2 link` 需要一份可解析的资源表。如果手头的 `android.jar` 资源表被裁剪过（部分发行版会裁剪），用完整平台包替代即可。

CI：`.github/workflows/build.yml` 在每次推送到 `main` 时自动构建，产物以 artifact 形式提供。

---

## 源码结构

| 文件 | 职责 |
|---|---|
| `HookEntry.java` | 入口，按包名分发，安装网络层与各方法层 hook |
| `Config.java` | 开关常量与跨进程读取（XSharedPreferences） |
| `MainActivity.java` | 分组开关面板、重启、抓取模式、日志查看与分享 |
| `assets/DISCLAIMER.txt` | 免责声明，应用内可查看 |

```
src/io/github/vivibibi30/pguard/   源码
res/values/                        app_name 与 xposed_scope 数组
assets/                            xposed_init、免责声明
libs/                              Xposed API 82（编译用）
docs/                              技术笔记与规则说明
build.sh                           一键构建
```

---

## 适配边界

- 拦截范围限于已经识别的路径与规则，不代表覆盖 HyperOS 的全部采集或联网行为
- 方法层依赖当前版本的类名与签名，系统组件升级后可能 `hook miss`；此时网络层仍然工作
- 使用 native 网络栈（如 Cronet）的请求不受 Java 层 hook 影响
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
| `rules/analytics/FuckMiuiUpload` 中 5 个 analytics 目标方法（`onetrack.p.u#w`、`c.f.k#y`、`onetrack.q.c#a/#b/#c`） | `hookAnalyticsMethods()`，改为按返回类型兜底，避免向基本类型方法塞 null |
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

### 第三方组件

| 组件 | 许可 | 用途 |
|---|---|---|
| [Xposed API 82](https://api.xposed.info/) | Apache-2.0 | 编译期依赖，`libs/api-82.jar`，不打包进模块 |

本项目不包含 DexKit、dexlib2 或任何 DEX 解析库，因此 APK 体积保持在约 30 KB。

## 许可

GPL-3.0，见 [LICENSE](LICENSE)。

使用前请阅读 [DISCLAIMER.txt](DISCLAIMER.txt)。

---

### English

Xposed module for MIUI / HyperOS that blocks data collection and uploads from
preinstalled system components: carrier telemetry, cloud app scanning, analytics,
installer stats, ads, and XSOF behaviour queries.

Interception happens at four stages — collection entry, local storage, IPC
interfaces, and network egress — with per-app switches. Requires root + LSPosed.
No DEX scanning; ~30 KB.

Personal use only. See [DISCLAIMER.txt](DISCLAIMER.txt).
