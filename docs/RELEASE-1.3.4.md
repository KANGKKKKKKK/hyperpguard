感谢您使用 HyperPGuard。

本次更新修复安装阶段中断、开关读不到与自检不准的问题。

## 修复

- dispatch 拆成逐段兜底：网络、隐私 API、XSOF、行为库、静默录屏各自独立安装，某一项抛错时其余继续
- 系统 Context 读 `Settings.Global` 被 AMS 拒绝时，改用目标包自己的 Context 重读一次；此前这类进程只能拿到内置默认值
- LSPosed 服务断开后自动重连，最多五次；覆盖安装模块后不必重开界面
- 自检新增总开关状态；「已注入进程」改为读 LSPosed 的 modules 日志；attach 无记录时区分「缓冲已滚出」与「未注入」
- 日志新增 `stage <名称> fail`、`net install fail`、`privacy install fail`，便于定位失败环节

## 说明

- 4.0 组件里 SystemHelper 的混淆类名变化时，方法层仍会 `hook miss`，网络层与隐私 API 层不受影响

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.3...v1.3.4
