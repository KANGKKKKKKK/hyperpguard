# HyperPGuard 1.1.0

[![Build APK](https://github.com/vivibibi30/pguard/actions/workflows/build.yml/badge.svg)](https://github.com/vivibibi30/pguard/actions/workflows/build.yml)

面向 MIUI / HyperOS 的隐私上报拦截模块（LSPosed）。预装系统组件会持续采集设备标识、应用使用记录、位置、通话行为等信息，并交给厂商、运营商或第三方。本模块在**采集入口、本地存储、对外接口、网络出口**四个环节设防，按应用分组独立开关。

本版增加隐私 API 层，并按「隐私优先、功能类默认关闭」重排开关。

---

## 拦截分四层

| 层 | 做法 | 例子 |
|---|---|---|
| 网络出口 | Hook `URL#openConnection`、`InetAddress#getByName`、`Socket` 构造与 `connect`，规则为「域名后缀 + 可选精确路径」 | `tracking.miui.com`、`dm.wo.com.cn`、`flash.sec.miui.com` |
| **隐私 API** | 在目标进程内按框架公开签名截断采集来源 | 应用列表、使用记录、IMEI/IMSI、短信发送、XMSF 代发广播 |
| 方法层 | 反射定位方法后直接返回 | SystemHelper 发送与入队、XSOF 写库与评分、analytics 使用记录聚合 |
| 跨进程接口 | XSOF Provider / AIDL 全入口拦截 | 第三方查不到截图、通话、屏幕共享记录 |

四层同时生效，任一层失效时其余层仍然工作。

## 隐私 API 层（1.1.0 新增）

| 目标 | 处理 | 作用进程 |
|---|---|---|
| `ApplicationPackageManager#getInstalledPackages/getInstalledApplications` | 返回空列表 | 小米统计、系统安全组件 |
| `UsageStatsManager#queryEvents/queryUsageStats` | `null` / 空列表 | 小米统计 |
| `TelephonyManager#getDeviceId/getImei/getMeid/getSubscriberId/getSimSerialNumber` | `null` | 小米统计、系统安全组件、安全中心、SystemHelper、运营商服务 |
| `SmsManager#sendTextMessage/sendMultipartTextMessage/sendDataMessage` | 不发送 | 运营商服务（短信注册兜底） |
| `Context#sendBroadcast`（action = `com.xiaomi.xmsf.push.XMSF_UPLOAD_ACTIVE`） | 丢弃 | 小米统计交给 XMSF 代发的材料 |

## 覆盖组件

| 组件 | 包名 | 拦截内容 |
|---|---|---|
| SystemHelper | `com.mobiletools.systemhelper` | 运营商遥测：设备 / SIM / 基站 / 应用列表 |
| 系统安全组件 | `com.miui.guardprovider` | 应用列表云查、AI 通话预警云检测、反诈配置；Flamingo 云查上传默认关闭 |
| 安全中心 | `com.miui.securitycenter` | XSOF 查询与写库、反诈配置、志愿者传感器上报；云查 / 鉴权 / 月报 / 防窥分类 / 地震报警定位 / ADB 验证默认关闭 |
| 小米统计 | `com.miui.analytics` | tracking / om.tracking / 区域与备份域 / sdkconfig 整域，含 DoH 旁路 |
| 应用安装器 | `com.miui.packageinstaller` | AVL 上报、活跃统计、安装页广告位 |
| 应用商店 | `com.xiaomi.market` | 广告配置下发 |
| 小米服务框架 | `com.xiaomi.xmsf` | 广告接口 |
| 运营商服务 | `com.xiaomi.registration` | 明文 CTEI 上报（默认拦）、设备注册（默认关） |
| 小米可信服务 | `com.xiaomi.trustservice` | MRM 认证链、设备证书/密钥上报（均默认关闭） |
| 翻译组件 | `com.xiaomi.aiasst.vision` | 语音识别上传（`/speech/`），默认关闭 |

## 界面预览

| 总开关 / 需要 root 的功能 | 分组卡片与子项 |
|---|---|
| ![主界面](https://raw.githubusercontent.com/vivibibi30/pguard/main/docs/images/ui-main.png) | ![分组卡片](https://raw.githubusercontent.com/vivibibi30/pguard/main/docs/images/ui-cards.png) |

每组右上角就是该应用在 LSPosed 里的作用域开关；组关闭时组内子项置灰且不可点击。会影响系统功能的规则一律默认关闭，副标题写明代价。

---

## 本版重点

- 新增**隐私 API 层**：应用列表、使用记录、设备标识、短信、XMSF 代发在数据来源处截断
- 「小米统计」改为整域拦截 + DoH 旁路阻断，方法层用签名扫描覆盖 6.33.0 的 `a.b.a.*` 改名
- 新增**运营商服务**分组：`typdm.tydevice.com` 明文 HTTP 上传 IMEI/MAC 默认拦
- 安全中心新增四个默认关闭的子项（防窥分类、地震/报警/EPS 定位、ADB 验证、以及原有的云查/鉴权/月报）
- 修复组门 `Config.on(key, false)` 恒 false 导致 **六段 hook 从未安装**的问题
- 版本号与发布流程整理：`1.1.0` / versionCode 3

## 从 1.0.1 到 1.1.0

| 项 | 1.0.1 | 1.1.0 |
|---|---|---|
| 拦截层次 | 网络 + 方法 + XSOF | 网络 + **隐私 API** + 方法 + XSOF |
| 小米统计 | 仅域名 | 整域 + 备份域 + DoH + 签名扫描方法层 |
| 运营商服务 | 无 | 新分组，明文 CTEI 默认拦 |
| 安全中心子项 | 3 项 | 8 项（5 项默认关闭） |
| 组门判断 | `fallback=false`，六段 hook 静默失效 | 统一 `true`，实测全部挂载成功 |
| 版本 | 1.0.1 / code 2 | 1.1.0 / code 3 |

## 安装

1. 安装本页附件 `HyperPGuard-1.1.0.apk`
2. 在 LSPosed 中启用「隐私上报拦截」
3. 打开模块，首次会向 LSPosed 请求全部作用域，允许即可；也可在 LSPosed 里手动勾选
4. 在模块里对目标应用点一次「重启」，让它重新加载即可生效

## 校验

```
大小      54035 字节
MD5       8449aa3be2278e540871212931d764c7
SHA256    4647d6996d8f192136a724497addddc887d4774af6e45451968f712b377a237e
签名证书  SHA-256 cacdf86d34ff7f7de0ed99467364ff5fc9d898f6c61d2202a02a34050d8cdcd3
```

```sh
apksigner verify --print-certs HyperPGuard-1.1.0.apk
sha256sum HyperPGuard-1.1.0.apk
```

附件由 CI 构建，字节哈希以 Release 附件页显示为准；证书指纹固定，可确认是不是同一把密钥签的。

## 已知限制

- 方法层类名随组件版本变化，日志里 `hook miss` 属正常现象，网络层与隐私 API 层不受影响
- 细粒度子项开关目前由界面记录，hook 侧按内置默认值执行，尚未接入 RemotePreferences
- 使用 native 网络栈（Cronet 等）的请求不受 Java 层 hook 影响
- `vendor.xiaomi.hardware.mrm.IMrm` 属 HAL 层，Java 无法直接拦截，仅从 `ServiceManager` 侧断开
- 拦截范围限于已识别的通道，不代表覆盖 HyperOS 全部联网行为

## 参考与致谢

以下项目的公开资料提供了识别目标与验证思路；本项目代码独立编写，未复制、未反编译复用其实现。

- **HyperCeiler**（GPL-3.0，Sevtinge）
- **XSOF Privacy**（yunyizea）
- **Hyper 隐私卫士**（`io.github.hyperprivacyguard`）
- **HyperOS4-XSOF-Analysis**（FAQQAQ）：整域范围、`om.tracking`、区域/备份域、DoH、`framework.sec`、`virus-engine`、地震/报警/EPS、`typdm.tydevice.com`、`a.b.a.*` 结构
- **libxposed API**（Apache-2.0）、**LSPosed**

感谢以上作者与维护者。

## 免责声明

仅供在本人拥有完全控制权的设备上学习研究使用，不做商业分发，不得移除声明后转载。模块自身不联网、不收集任何数据；可能影响依赖同一接口的系统功能（设备证书、查找设备、病毒库更新、语音识别、地震预警、报警定位），默认关闭的项目请自行评估后开启。

完整八条条款见 [DISCLAIMER.txt](https://github.com/vivibibi30/pguard/blob/main/DISCLAIMER.txt)。本项目以 GPL-3.0 发布。
