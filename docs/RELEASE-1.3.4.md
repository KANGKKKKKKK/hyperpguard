感谢您使用 HyperPGuard。

本次更新让单个 hook 失败不再影响同进程其余规则，并修复部分进程读不到开关的问题。

## 修复

- dispatch 阶段拆开兜底：网络、隐私 API、XSOF、行为库、静默录屏各自独立安装，某一项抛错时其余继续
- 在系统 Context 下读 `Settings.Global` 被 AMS 拒绝（`Package android does not belong to <uid>`）时，改用目标包自己的 Context 重读一次；此前这类进程只能拿到内置默认值
- 日志新增 `stage <名称> fail` 与 `net install fail` / `privacy install fail`，便于定位具体失败环节

## 说明

- 4.0 组件里 SystemHelper 的混淆类名变化时，方法层仍会 `hook miss`，网络层与隐私 API 层不受影响

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.3...v1.3.4
