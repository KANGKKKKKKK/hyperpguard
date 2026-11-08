<div align="center">

# HyperPGuard

![license](https://img.shields.io/badge/license-GPL--3.0-blue)
![language](https://img.shields.io/badge/language-Java-orange)
![release](https://img.shields.io/github/v/release/vivibibi30/hyperpguard?label=release&color=green)
![android](https://img.shields.io/badge/Android-10--16-green)
![status](https://img.shields.io/badge/status-按需维护-2da44e?style=flat-square)

在采集入口、本地存储、对外接口、网络出口四层拦截 HyperOS 预装组件的隐私上报

</div>

预装系统组件会持续采集设备标识、应用使用记录、位置、通话行为等信息，并通过网络或跨进程接口提供给厂商、运营商或第三方。本模块在这些通道上按应用分组拦截，开关与 LSPosed 作用域联动。

> **维护节奏**：本项目由个人维护，开发与验证依赖自费的模型调用与真机测试。因此只在出现影响使用的缺陷、发现新的上报通道或系统大版本适配时发版；界面与文案类改动会并入下一次实质更新，不再单独出包。

本仓库即源码仓：`src/` 下是全部 Java 实现，`build.sh` 是完整构建脚本，用 `javac` + `d8` + `aapt2` + `zipalign` + `apksigner` 打包，不依赖 Gradle；`Releases` 里的 APK 由它构建。`Xposed-Modules-Repo/io.github.vivibibi30.hyperpguard` 只是 LSPosed 的索引仓库，只放 APK 与简介。

> 仅供个人在自有设备上学习研究使用。使用前请阅读 [DISCLAIMER.txt](DISCLAIMER.txt)。

## 当前支持的版本

- Android 10 – Android 16 的 HyperOS
- LSPosed 2.2.x（libxposed API 100+）
- Root：KernelSU / Magisk / APatch（「重启」「查看日志」需要）

## 安装

1. 安装 `HyperPGuard`
2. 在 LSPosed 中启用「HyperPGuard」
3. 打开应用，首次会向 LSPosed 请求全部作用域，允许即可；也可在 LSPosed 里手动勾选
4. 在模块里对目标应用点一次「重启」，或点批量行的「全部重启」，让它重新加载

作用域外的应用不会被注入。

## 使用

**分组开关**：每张卡片右上角是该应用的开关，等同于它在 LSPosed 里的作用域开关。组关闭时组内子项置灰不可操作。

**子项开关**：可以只留某条规则，例如只要 XSOF 拦截就把域名层那项关掉。会影响系统功能的规则一律默认关闭，副标题写明代价。

**重启**：改完开关点「重启」执行 `am force-stop`，应用下次启动重新注入；批量行有「全部重启」。系统常驻组件被停止后通常几秒内会被系统拉起，等于自动完成重启。

**查看日志 / 分享日志**：读取本地 logcat 中本模块的输出，可导出分享。日志不包含电话号码、行为库内容或调用参数。

**记录未拦截访问**：记录「访问了但没被规则命中」的域名，用于补规则，平时保持关闭。

**关于 / 检查更新**：卡片底部列出 GitHub 仓库、LSPosed 模块页与 Issues 入口，点「检查更新」会向 GitHub API 取最新 release tag 与当前版本比对；不点就不发任何请求。

## 拦截层次

| 层 | 做法 |
|---|---|
| 网络出口 | Hook `URL#openConnection`、`InetAddress#getByName`、`Socket` 构造与 `connect`，规则为「域名后缀 + 可选路径」 |
| 隐私 API | 应用列表、使用记录、设备标识、短信发送、XMSF 代发广播，在数据来源处截断 |
| 行为库 | 按 `screenshot` / `screen_share` / `call_forwarding` / `airplane_mode` / `fraud_phone` / `voip_record` / `fraud_url` / `install_app` 八张表名拦 `SQLiteDatabase` 写入 |
| 静默录屏 | `MediaProjection#createVirtualDisplay` 不建立 |
| 方法层 | 反射定位方法后直接返回：SystemHelper 发送与入队、analytics 使用记录聚合 |
| XSOF | Provider / AIDL / 写库 / 评分四层，第三方查不到截图、通话、屏幕共享记录 |

方法层与网络层互不依赖：类名变化时域名规则仍在工作，域名更换时方法层仍在工作，日志能分辨是哪一层命中。

## 覆盖组件

| 组件 | 包名 | 拦截内容 |
|---|---|---|
| SystemHelper | `com.mobiletools.systemhelper` | 运营商遥测；默认整进程断网（无用户功能）；定位 / 基站 / Wi-Fi 扫描默认关 |
| 系统安全组件 | `com.miui.guardprovider` | 应用列表云查、AI 通话预警云检测、反诈配置（AI 反诈逻辑 `AiGuardService` 在本组件内，用于通话场景）；静默录屏拦截默认开，Flamingo 云查上传默认关；定位 / Wi-Fi / 通讯录通话默认关 |
| 安全中心 | `com.miui.securitycenter` | XSOF 查询与写库、行为库写入、反诈配置、志愿者传感器上报；云查 / 鉴权 / 月报 / 防窥分类 / 地震报警定位 / ADB 验证 / 设备标识默认关；定位、通讯录 / 通话 / 短信默认关 |
| 小米统计 | `com.miui.analytics` | 整域 + DoH 旁路；默认整进程断网 |
| 应用安装器 | `com.miui.packageinstaller` | AVL 上报、活跃统计、安装页广告位 |
| 应用商店 | `com.xiaomi.market` | 广告配置下发、OneTrack 统计上报（`tracking.miui.com/track/`） |
| 小米服务框架 | `com.xiaomi.xmsf` | 广告接口、OneTrack 统计上报（`tracking.miui.com/track/`） |
| 运营商服务 | `com.xiaomi.registration` | 明文 CTEI 上报（默认拦）、设备注册（默认关）；定位 / 基站默认关 |
| 移动 DM | `com.miui.dmregservice` | `fxltsbl.com` 上报、App 清单、SIM 标识；基站 / Wi-Fi 扫描默认关 |
| 系统诊断 | `com.miui.misightservice` | 日志上传 `feedback.miui.com/misight/upload`、售后检测 `port.sec.miui.com/mqsas`、客服诊断 `chat.kefu.mi.com/openapi/misight/diagnostic`；触发条件是用户主动反馈或售后远程诊断，本地记录不等于已上传 |
| 小米可信服务 | `com.xiaomi.trustservice` | MRM 认证链、设备证书/密钥上报（均默认关） |
| 翻译组件 | `com.xiaomi.aiasst.vision` | 语音识别上传（`/speech/`），默认关 |

## 界面预览

| 总开关 / 批量操作 | 分组卡片 |
|---|---|
| ![主界面](docs/images/ui-main.png) | ![分组卡片](docs/images/ui-cards.png) |

每组右上角就是该应用在 LSPosed 里的作用域开关；组关闭时组内子项置灰且不可点击。

## 日志说明

| 前缀 | 含义 |
|---|---|
| `attach <包名>` | 模块注入某进程成功 |
| `X(host)` | 域名规则命中 |
| `X(host+path)` | 域名 + 路径规则命中 |
| `X(strict)` | 整进程断网生效 |
| `? <tag> <host>` | 「记录未拦截访问」记录：访问未被拦截 |
| `hook ok` / `hook miss` | 方法层安装成功 / 类名或签名不匹配 |
| `xsof provider hooked` | XSOF Provider 三个入口已拦截 |
| `xsof aidl hooked=N` | XSOF AIDL 接口拦截数量 |
| `xsof db write hooked=N` | XSOF 写库方法拦截数量 |
| `xsof score hooked=N` | 风险评分方法拦截数量 |
| `analytics scan scanned=N hooked=N` | analytics 签名扫描结果 |

## 如何确认生效

1. 启用模块，并对目标应用点一次「重启」
2. 触发一次它的上报行为（例如打开安全中心、安装一个应用）
3. 点「查看日志」，应看到 `attach <包名>` 与 `X(...)` 记录
4. XSOF 部分应全是 `hooked`，没有 `miss`
5. 只有 `attach` 没有 `X(...)` 时，打开「记录未拦截访问」跑一段时间，把 `?` 开头的域名补进规则

## 构建

需要 JDK 17 与 build-tools 35 及以上，SDK 路径用环境变量指定：

```sh
export ANDROID_JAR=/path/to/android.jar D8=/path/to/d8 AAPT2=/path/to/aapt2
export ZIPALIGN=/path/to/zipalign APKSIGNER=/path/to/apksigner
sh build.sh
```

## 源码结构

```
src/io/github/vivibibi30/hyperpguard/
├── HookEntry.java      入口与按包分发（XposedModule#onPackageReady）
├── Config.java         开关常量
├── NetGuard.java       网络出口：URL / InetAddress / Socket + 规则匹配
├── PrivacyApis.java    隐私 API：应用列表 / 使用记录 / 标识 / 短信 / XMSF 广播
├── CaptureHooks.java   行为库写入与静默录屏
├── XsofHooks.java      XSOF Provider / AIDL / 写库 / 评分 + MRM
├── AnalyticsHooks.java 统计方法层与签名扫描
├── Hooks.java          公共 hook 工具
├── Logs.java           统一日志标签
└── MainActivity.java   设置界面
```

```
res/values/           app_name 等字符串资源
assets/               免责声明
META-INF/xposed/      模块声明（必须打到 APK 根）
libs/libxposed-api.jar       libxposed API（compileOnly，不打进 dex）
libs/libxposed-service.jar   service + interface 合并版（必须打进 dex）
libs/api-82.jar              传统 Xposed API（编译兼容用）
docs/                 技术笔记、规则说明、截图
build.sh              一键构建
```

## 适配边界

- 域名名单可能漏项：地址可由配置下发；SystemHelper 与小米统计为整进程断网，其余组件依赖隐私 API 层与方法层
- 方法层类名随组件版本变化，日志里 `hook miss` 属正常，网络层与隐私 API 层不受影响
- 使用 native 网络栈（Cronet 等）的请求不受 Java 层 hook 影响
- `IMrm` 属 HAL 层，仅从 `ServiceManager` 侧断开
- `com.xiaomi.xmsf` 代发属于跨进程交接，只拦 analytics 自身 UID 覆盖不到
- 拦截范围限于已识别的通道，不代表覆盖 HyperOS 全部联网行为
- `com.mobiletools.systemhelper` 的权限在新机激活（OOBE）阶段授予，激活时的权限弹窗值得逐项看清；本模块只拦上报，不回收已授予的权限。对授权有疑虑时，可在设置里找「隐私与安全 / 反馈 / 用户体验计划」这类入口，或备份后重置手机重走开机引导
- 识别结论以静态分析与真机行为对照为准，单看一段静态代码不足以定性
- AI 反诈逻辑位于 `com.miui.guardprovider` 的 `AiGuardService`，用于通话场景；`com.mobiletools.systemhelper` 承担运营商侧的数据上报，两者不是同一条链路

## 常见问题

- **某个应用开关打不开**：该包可能不存在、被冻结或与系统共享 UID，LSPosed 无法把它加入作用域
- **日志为空**：`logcat` 环形缓冲已覆盖，或目标应用还没重启过
- **功能异常**：对应的默认关闭子项被打开了，按需关掉即可
- **方法层出现 `hook miss`**：组件版本变化，网络层与隐私 API 层不受影响

## 反馈与边界

- 提 issue 前先点「自检」，日志脱敏后再贴；具体见 [CONTRIBUTING.md](CONTRIBUTING.md)
- 不要上传厂商组件的 APK 或反编译产物到 issue，需要分析时请在自己设备上用 `pm path` 取
- 本项目不做付费分发，也不提供「赞助解锁功能」；第三方 fork 按 GPL-3.0 自行处理

## 更新日志

### 1.5.0

- 新增「自定义规则」：填域名或 域名/路径 追加拦截，对所有作用域应用生效；添加或删除后需重启对应应用
- 「未拦截」行按内置表标注用途（统计埋点、广告位、引擎日志、网络助手等），方便判断要不要加规则
- 安全中心新增「广告位与推送下发」：`adv.sec.miui.com/info/layout`、网络助手推送、满意度问卷；系统安全组件新增「引擎日志上传」：`logupdate.avlyun.sec.miui.com`

### 1.4.0

- 新增「行为库状态」：读安全中心 `fraud_detection_data.db` 的 `sqlite_sequence`，显示各表累计写入次数，刷新后对增长标注 (+N)
- 只读计数，不读通话号码与截图路径；复制到应用私有目录读完即删
- 应用商店与小米服务框架新增「使用统计上报」：拦 `tracking.miui.com/track/`，只影响统计，不影响下载、推送与登录

### 1.3.9

- 「查看日志」按「未拦截 / 已拦截 / 模块日志」分段显示，不再混在一起

### 1.3.8

- 修复「查看日志」在 Android 13 及以上始终为空：logcat 的 tag 从 `I/HyperPGuard:` 变成 `I/HyperPGuard( 1234):`，原过滤条件匹配不到
- 自检的「安装失败记录」与 attach 记录同步修正

### 1.3.7

- 分享出去的标题改为「运行日志」，分享选择器同步
- 自检不再附带粘贴提示

