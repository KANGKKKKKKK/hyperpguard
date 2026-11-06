# PrivacyGuard 1.0.1

面向 MIUI / HyperOS 的隐私上报拦截模块。在「采集入口 → 本地存储 → 对外接口 → 网络出口」四个环节拦截预装组件的数据外泄，按应用分组独立开关。

本版把模块从传统 Xposed API 82 迁移到 **libxposed API 102**，作用域管理改走 LSPosed 官方接口，并修掉一批界面与构建问题。

---

## 本版重点

- **接口层重写**：入口改为 `XposedModule` + `onPackageReady`，按包名分发，hook 用 `hook(method).intercept(chain -> ...)`；直接返回值即跳过原方法，`chain.proceed()` 为放行。
- **作用域走官方接口**：`getScope()` 读、`requestScope()` / `removeScope()` 写，不再改动 `modules_config.db`。
- **界面状态即真实状态**：分组开关在服务绑定和每次回到界面时按真实作用域回填，不再由本地默认值决定。
- **总开关关闭即锁子项**：该应用的总开关关掉后，其下子项置灰且不可操作；打开后恢复。
- **root 提示归位**：「重启」「查看记录」需要 root，提示写在设置卡片里，应用启动不再自动执行 `su`。
- **构建可复现**：固定签名密钥并校验证书指纹，`META-INF/xposed/*` 打进 APK 根目录，CI 换用 build-tools 35。

## 变更明细

| 项 | 1.0.0 | 1.0.1 |
|---|---|---|
| 模块 API | Xposed API 82 | libxposed API 102 |
| 入口 | `IXposedHookLoadPackage` | `XposedModule#onPackageReady` |
| 作用域 | 手动在 LSPosed 勾选 / 改数据库 | `requestScope` / `removeScope` / `getScope` |
| 分组开关状态 | 本地默认值（固定 false） | LSPosed 真实作用域回填 |
| 子项与总开关 | 恒为灰色、不可点 | 随总开关联动，关闭时置灰禁用 |
| root 检测 | 启动时自动 `su` | 仅在点「重启」「查看记录」时触发 |
| 构建 | build-tools 34、密钥可能每次重新生成 | build-tools 35、固定密钥 + 指纹校验 |
| APK 体积 | 约 33 KB | 约 49 KB（含 libxposed service 与 AIDL） |

## 支持环境

| 项目 | 实际情况 |
|---|---|
| 系统 | Android 16 / HyperOS（minSdk 29） |
| 框架 | LSPosed 2.2.1，libxposed API 100+ |
| 权限 | KernelSU / Magisk / APatch 任一 |
| 已验证组件 | 系统安全组件 2.5.6、安全中心 12.8.6、SystemHelper 3.0.1、应用安装器 |

## 安装

1. 安装本页附件 `PrivacyGuard-1.0.1.apk`
2. 在 LSPosed 中启用「隐私上报拦截」
3. 打开模块，首次会向 LSPosed 请求全部作用域，允许即可；也可在 LSPosed 里手动勾选
4. 重启手机，或在模块卡片上对目标应用点「重启」

目标应用未进入作用域时不会注入。

## 校验

```
文件    PrivacyGuard-1.0.1.apk
大小    49939 字节
MD5     6b2dcd5c9c3d705b8185d1d056236463
SHA256  886eb30f1e366a4689f79d3ed45fe1496a2ee1e69e0b7cb2f6e66dc268b0c0f7
签名证书 SHA-256
        cacdf86d34ff7f7de0ed99467364ff5fc9d898f6c61d2202a02a34050d8cdcd3
```

```sh
apksigner verify --print-certs PrivacyGuard-1.0.1.apk
sha256sum PrivacyGuard-1.0.1.apk
```

## 已知限制

- 细粒度子项开关目前仍是界面侧记录，hook 侧按内置默认值执行，尚未接入 RemotePreferences（下一版处理）
- 方法层依赖系统组件的混淆类名，组件升级后可能 `hook miss`；此时域名层仍然工作
- 使用 native 网络栈（Cronet 等）的请求不受 Java 层 hook 影响
- 拦截范围限于已识别的域名与路径，不代表覆盖 HyperOS 全部联网行为

## 参考与致谢

拦截目标与识别方式参考了 HyperCeiler（GPL-3.0，Sevtinge）、XSOF Privacy（yunyizea）、Hyper 隐私卫士等项目的公开资料，全部代码为独立编写。作用域与模块入口基于 [libxposed API](https://github.com/libxposed/api)（Apache-2.0）。

本项目以 GPL-3.0 发布，使用前请阅读 [DISCLAIMER.txt](../DISCLAIMER.txt)。
