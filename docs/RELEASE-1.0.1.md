# HyperPGuard 1.0.1

[![Build APK](https://github.com/vivibibi30/pguard/actions/workflows/build.yml/badge.svg)](https://github.com/vivibibi30/pguard/actions/workflows/build.yml)

面向 MIUI / HyperOS 的隐私上报拦截模块（LSPosed）。预装系统组件会持续采集设备标识、应用使用记录、位置、通话行为等信息，并交给厂商、运营商或第三方。本模块在**采集入口、本地存储、对外接口、网络出口**四个环节设防，按应用分组独立开关。

---

## 拦截在哪几层

| 环节 | 做法 | 效果 |
|---|---|---|
| 网络出口 | Hook `URL#openConnection`、`InetAddress#getByName`、`Socket` 构造与 `connect`，规则为「域名后缀 + 可选精确路径」 | 命中即抛异常，连接不建立 |
| 对外接口 | XSOF 的 Provider 三入口（`query` / `call` / `insert`）、AIDL 7 个方法、风险评分 | 第三方查不到截图 / 通话 / 屏幕共享记录 |
| 本地存储 | XSOF 写库的 27 个方法 | 不再产生新的行为记录 |
| 方法层 | SystemHelper 发送与入队、trustservice 的 MRM 服务 | 直接返回空值，不进队列、不建连接 |

四层同时生效，任一层失效时其余层仍然工作。

## 覆盖组件

| 组件 | 包名 | 拦截内容 |
|---|---|---|
| SystemHelper | `com.mobiletools.systemhelper` | 运营商遥测：设备 / SIM / 基站 / 应用列表 |
| 系统安全组件 | `com.miui.guardprovider` | 应用列表云查、AI 通话预警云检测、反诈配置拉取 |
| 安全中心 | `com.miui.securitycenter` | XSOF、反诈配置、志愿者传感器上报；云查 / 鉴权 / 月报 / 防窥分类 / 地震报警定位 / ADB 验证默认关闭 |
| 小米统计 | `com.miui.analytics` | tracking / om.tracking / sdkconfig 整域，含 DoH 旁路 |
| 应用安装器 | `com.miui.packageinstaller` | AVL 上报、活跃统计、安装页广告位 |
| 应用商店 | `com.xiaomi.market` | 广告配置下发 |
| 小米服务框架 | `com.xiaomi.xmsf` | 广告接口 |
| 小米可信服务 | `com.xiaomi.trustservice` | MRM 认证链、设备证书/密钥上报（均默认关闭） |
| 翻译组件 | `com.xiaomi.aiasst.vision` | 语音识别上传（默认关闭） |

## 界面预览

| 总开关 / 需要 root 的功能 | 分组卡片与子项 |
|---|---|
| ![主界面](https://raw.githubusercontent.com/vivibibi30/pguard/main/docs/images/ui-main.png) | ![分组卡片](https://raw.githubusercontent.com/vivibibi30/pguard/main/docs/images/ui-cards.png) |

每组右上角就是该应用在 LSPosed 里的作用域开关；组关闭时组内子项置灰且不可点击。

---

## 本版重点

- **迁移到 libxposed API 102**：入口改为 `XposedModule#onPackageReady`，hook 用 `hook(method).intercept(chain -> ...)`，直接返回值即跳过原方法
- **作用域走官方接口**：`getScope()` 读、`requestScope()` / `removeScope()` 写，不再改动 `modules_config.db`
- **界面状态即真实状态**：分组开关在服务绑定与每次回到界面时按真实作用域回填
- **总开关关闭即锁子项**：该应用关闭后，其下子项置灰且不可操作
- **root 提示归位**：「重启」「查看记录」需要 root，提示写在设置卡片，启动不再自动 `su`
- **构建可复现**：固定签名密钥并校验指纹，`META-INF/xposed/*` 打进 APK 根目录，CI 使用 build-tools 35

## 变更明细

| 项 | 1.0.0 | 1.0.1 |
|---|---|---|
| 模块 API | Xposed API 82 | libxposed API 102 |
| 入口 | `IXposedHookLoadPackage` | `XposedModule#onPackageReady` |
| 作用域 | 手动在 LSPosed 勾选 / 改数据库 | `requestScope` / `removeScope` / `getScope` |
| 分组开关状态 | 本地默认值 | LSPosed 真实作用域回填 |
| 子项与总开关 | 恒为灰色 | 随总开关联动，关闭时置灰禁用 |
| root 检测 | 启动时自动 `su` | 仅在点「重启」「查看记录」时触发 |
| 构建 | build-tools 34、密钥可能每次重生成 | build-tools 35、固定密钥 + 指纹校验 |
| APK 体积 | 约 33 KB | 约 49 KB（含 libxposed service 与 AIDL） |

## 使用

- **分组开关**：每张卡片右上角是该应用的开关，等同于它的 LSPosed 作用域
- **子项开关**：可以只留某条规则，例如只要 XSOF 拦截就把域名层那项关掉
- **重启**：改完开关点卡片上的「重启」即执行 `am force-stop`，应用下次启动重新注入；系统常驻组件通常几秒后被系统拉起
- **查看记录 / 分享日志**：读取本地 logcat 中本模块的输出，可导出分享；日志不包含电话号码、行为库内容或调用参数
- **抓取模式**：记录「访问了但没被规则命中」的域名，用于补规则，平时保持关闭

## 安装

1. 安装本页附件 `HyperPGuard-1.0.1.apk`
2. 在 LSPosed 中启用「隐私上报拦截」
3. 打开模块，首次会向 LSPosed 请求全部作用域，允许即可；也可在 LSPosed 里手动勾选
4. 在模块里对目标应用点一次「重启」即可生效

## 校验

```
大小      49939 字节
签名证书  SHA-256 cacdf86d34ff7f7de0ed99467364ff5fc9d898f6c61d2202a02a34050d8cdcd3
```

```sh
apksigner verify --print-certs HyperPGuard.apk   # 证书指纹应一致
sha256sum HyperPGuard.apk                        # 与下载页显示的 sha256 对照
```

附件由 CI 构建，字节哈希会随构建变化，以 Release 附件页显示的 `sha256:` 为准；证书指纹固定不变，可用来确认是不是同一把密钥签的。

## 常见问题

- **日志里只有 `attach`，没有 `X(...)`**：规则没命中。打开抓取模式跑一段时间，把 `?` 开头的域名补进规则
- **出现 `hook miss`**：方法层类名或签名随组件升级变化，域名层仍然工作，可提 Issue 附版本号
- **查找设备 / 病毒库更新异常**：检查「小米可信服务」「系统安全组件」里与 MRM、病毒库相关的项，按需关闭
- **看不到历史记录**：记录来自 logcat 环形缓冲，被覆盖后就没有了
- **装完没反应**：确认模块已在 LSPosed 启用、目标应用在作用域内，并点过一次「重启」

## 已知限制

- 细粒度子项开关目前由界面记录，hook 侧按内置默认值执行，尚未接入 RemotePreferences（下一版处理）
- 使用 native 网络栈（Cronet 等）的请求不受 Java 层 hook 影响
- 拦截范围限于已识别的域名与路径，不代表覆盖 HyperOS 全部联网行为
- 停用模块后行为立即恢复，不修改系统 APK、不删除既有数据

## 参考与致谢

- **HyperCeiler**（GPL-3.0，Sevtinge）
- **XSOF Privacy**（yunyizea）
- **Hyper 隐私卫士**（`io.github.hyperprivacyguard`）
- **libxposed API**（Apache-2.0）、**LSPosed**

代码独立编写，未复制、未反编译复用上述项目的实现。

## 免责声明

仅供在本人拥有完全控制权的设备上学习研究使用，不做商业分发，不得移除声明后转载。模块自身不联网、不收集任何数据；可能影响依赖同一接口的系统功能（设备证书、查找设备、病毒库更新、语音识别），默认关闭的项目请自行评估。

完整八条条款见 [DISCLAIMER.txt](https://github.com/vivibibi30/pguard/blob/main/DISCLAIMER.txt)。本项目以 GPL-3.0 发布。
