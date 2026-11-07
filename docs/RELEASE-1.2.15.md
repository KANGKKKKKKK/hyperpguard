感谢您使用 HyperPGuard。

本次更新补齐了移动 DM 组件，敏感数据开关改为按应用独立。

## 新增

- 新增 `com.miui.dmregservice` 覆盖：拦截 `fxltsbl.com`，以及 App 清单、SIM 标识、基站、Wi-Fi 扫描的采集
- 新增「隐藏桌面图标」开关：桌面入口改为 activity-alias，隐藏后仍可从 LSPosed 模块页进入

## 优化 / 修复

- 敏感数据拆分为按应用独立开关：定位、基站、Wi-Fi 扫描、通讯录/通话/短信分别归入对应应用卡片，打开一项只影响该应用
- 修复「屏蔽 MRM 认证链」开关语义相反：此前关闭时反而屏蔽 `ServiceManager` 的 MRM 服务
- 修复本机配置被清理后打开任意开关会把其余开关冲成默认值
- 修复关闭全部网络子项时，guardprovider 的隐私 API 与静默录屏拦截一并失效
- 修复移动 DM 的组总开关与子项共用同一个开关
- 修复「拦截记录」把其他进程里含包名的日志当成拦截条目列出

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.14...v1.2.15
