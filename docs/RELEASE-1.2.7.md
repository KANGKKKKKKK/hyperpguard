感谢您使用 HyperPGuard。

本次更新收紧了权限，并让开关在所有进程里都能读到。

## 优化

- 开关改存系统设置 `Settings.Global`，不再向 `/data/local/tmp` 落文件
- 目标进程改用自己进程的 Context 读取开关，应用商店、安装器、SystemHelper 等普通应用不再回落到内置默认值
- 多进程应用在 Application 创建后再读一次，子进程也能拿到同一份配置

## 安全性

- 移除对外暴露的 `ConfigProvider`，任何应用都无法再查询开关状态
- 对外组件只剩 libxposed 规范要求的 `XposedProvider` 与桌面入口 `MainActivity`

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.6...v1.2.7
