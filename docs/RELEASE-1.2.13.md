感谢您使用 HyperPGuard。

本次更新让作用域请求失败时能看清原因。

## 优化

- 加入作用域被 LSPosed 拒绝时，从 Toast 改为对话框：显示包名、LSPosed 返回的原始信息，以及手动勾选的操作路径
- 系统 priv-app（例如 SystemHelper）常见这种拒绝，此前提示一闪而过，容易被当成「开关自己关了」

## 说明

- 拦截逻辑与上一版一致

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.12...v1.2.13
