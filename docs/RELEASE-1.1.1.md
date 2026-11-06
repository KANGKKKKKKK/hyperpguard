# PrivacyGuard 1.1.1

[![Build APK](https://github.com/vivibibi30/pguard/actions/workflows/build.yml/badge.svg)](https://github.com/vivibibi30/pguard/actions/workflows/build.yml)

MIUI / HyperOS 隐私上报拦截模块（LSPosed）。在采集入口、本地存储、对外接口、网络出口四个环节拦截预装组件的数据外泄，按应用分组独立开关。

## 变更（对比 1.1.0）

| 组件 | 1.1.0 | 1.1.1 |
|---|---|---|
| SystemHelper | 拦截 `dm.wo.com.cn` 等规则 | 新增「全断网」子项（默认开）：该进程所有非本机请求全部拦截 |
| 小米统计 | 整域拦截 + DoH 旁路 | 新增「全断网」子项（默认开） |
| 其余组件 | — | 不变 |

两个「全断网」作用在目标进程内部（`URL#openConnection` / `InetAddress` / `Socket`），不涉及 root 或 iptables。

## 覆盖组件

| 组件 | 包名 | 处理 |
|---|---|---|
| SystemHelper | `com.mobiletools.systemhelper` | 整进程断网 + 方法层 |
| 系统安全组件 | `com.miui.guardprovider` | 域名 + 隐私 API 层 |
| 安全中心 | `com.miui.securitycenter` | XSOF 四层 + 按需子项 |
| 小米统计 | `com.miui.analytics` | 整进程断网 + 签名扫描方法层 |
| 应用安装器 | `com.miui.packageinstaller` | 路径规则 |
| 应用商店 / 服务框架 | `com.xiaomi.market` / `com.xiaomi.xmsf` | 广告接口 |
| 运营商服务 | `com.xiaomi.registration` | 明文 CTEI（默认拦）、设备注册（默认关） |
| 小米可信服务 | `com.xiaomi.trustservice` | MRM、设备证书（默认关） |
| 翻译组件 | `com.xiaomi.aiasst.vision` | `/speech/`（默认关） |

## 校验

```
大小      54035 字节
MD5       a746a40632286ec2132be3a145b30037
SHA256    775afa86b48fac52cecff19df1c9186d0c964bb18dbf2db391f7d09c4a7148c4
签名证书  SHA-256 cacdf86d34ff7f7de0ed99467364ff5fc9d898f6c61d2202a02a34050d8cdcd3
```

附件由 CI 构建，字节哈希以下载页显示的为准；证书指纹固定。

## 已知限制

- 域名名单可能漏项，地址可由 `sdkconfig` 配置下发
- 方法层类名随组件版本变化，`hook miss` 属正常
- 细粒度子项开关由界面记录，hook 侧按内置默认值执行
- `IMrm` 属 HAL 层，仅从 `ServiceManager` 侧断开
- `com.xiaomi.xmsf` 代发属于跨进程交接，只拦 analytics 自身 UID 覆盖不到

## 参考与致谢

- [HyperCeiler](https://github.com/Sevtinge/HyperCeiler)（GPL-3.0，Sevtinge）
- [XSOF Privacy](https://modules.lsposed.org/module/io.github.yunyizea.xsofprivacy)（yunyizea）
- Hyper 隐私卫士（`io.github.hyperprivacyguard`）
- [HyperOS4-XSOF-Analysis](https://github.com/FAQQAQ/HyperOS4-XSOF-Analysis)（FAQQAQ）
- [libxposed API](https://github.com/libxposed/api)（Apache-2.0）、LSPosed

代码独立编写，未复制、未反编译复用上述项目的实现。

## 免责声明

仅供在本人拥有完全控制权的设备上学习研究使用，不做商业分发，不得移除声明后转载。完整条款见 [DISCLAIMER.txt](https://github.com/vivibibi30/pguard/blob/main/DISCLAIMER.txt)。GPL-3.0。
