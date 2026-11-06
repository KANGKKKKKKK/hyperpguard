# HyperPGuard 1.2.0

[![Build APK](https://github.com/vivibibi30/hyperpguard/actions/workflows/build.yml/badge.svg)](https://github.com/vivibibi30/hyperpguard/actions/workflows/build.yml)

## 变更（对比 1.1.2）

| 项 | 1.1.2 | 1.2.0 |
|---|---|---|
| 模块名 | PrivacyGuard | HyperPGuard |
| 包名 | `io.github.vivibibi30.pguard` | `io.github.vivibibi30.hyperpguard` |
| 应用名 | 隐私上报拦截 | HyperPGuard |
| 功能 | — | 与 1.1.2 相同 |

包名变了，属于新的模块身份：安装后需要在 LSPosed 里重新启用并勾选作用域，旧包名的版本不会覆盖升级。

## 功能（与 1.1.2 一致）

- 网络出口：`URL` / `InetAddress` / `Socket` 三处拦截，规则为「域名后缀 + 可选路径」
- 隐私 API：应用列表、使用记录、设备标识、短信发送、XMSF 代发广播
- 行为库：`SQLiteDatabase` 按 8 张表名拦截写入
- 静默录屏：`MediaProjection#createVirtualDisplay` 不建立
- XSOF：Provider / AIDL / 写库 / 评分四层
- 整进程断网：SystemHelper、小米统计

## 校验

```
签名证书  SHA-256 cacdf86d34ff7f7de0ed99467364ff5fc9d898f6c61d2202a02a34050d8cdcd3
```

附件由 CI 构建，字节哈希以 Release 附件页为准；证书指纹固定。

## 参考与致谢

- **HyperCeiler**（GPL-3.0，Sevtinge）
- **XSOF Privacy**（yunyizea）
- **Hyper 隐私卫士**（`io.github.hyperprivacyguard`）
- **HyperOS4-XSOF-Analysis**（FAQQAQ）
- **OSAKOo**（酷安）— HyperOS 行为数据库与静默录屏复核
- **libxposed API**（Apache-2.0）、**LSPosed**

代码独立编写，未复制、未反编译复用上述项目的实现。

## 免责声明

仅供在本人拥有完全控制权的设备上学习研究使用，不做商业分发，不得移除声明后转载。完整条款见 [DISCLAIMER.txt](https://github.com/vivibibi30/hyperpguard/blob/main/DISCLAIMER.txt)。GPL-3.0。
