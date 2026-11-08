感谢您使用 HyperPGuard。

本次更新给安装器补了两个可选开关。

## 新增

- 「风险校验查询」：拦 `verify.sec.xiaomi.com` 与 `tz.sec.xiaomi.com`。这两条会带上待装应用的包名与 APK 哈希；打开后安装风险检测失效，包名与哈希不再外发
- 「游戏防篡改检查」：拦 `/v2|v3|v4/game/interceptcheck` 及其 normal / safemode / enhancemode 变体；打开后游戏安装时的完整性校验失效

## 说明

- 两项均默认关闭：会削弱检测能力，按需开启
- 安装器现有七个独立开关，互不影响

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.9...v1.5.10
