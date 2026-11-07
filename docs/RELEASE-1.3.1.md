感谢您使用 HyperPGuard。

本次更新在卡片里补了两行说明，避免把不同组件的能力混在一起。

## 说明

- SystemHelper 的权限在新机激活（OOBE）阶段授予；本模块只拦上报，不回收已授予的权限
- AI 反诈逻辑在系统安全组件（`com.miui.guardprovider`）的 `AiGuardService` 内，用于通话场景；SystemHelper 承担运营商侧的数据上报，两者不是同一条链路
- 对授权有疑虑时，可在设置里找「隐私与安全 / 反馈 / 用户体验计划」这类入口，或备份后重置手机重走开机引导

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.0...v1.3.1
