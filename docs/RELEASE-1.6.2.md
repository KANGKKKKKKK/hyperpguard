感谢您使用 HyperPGuard。

修复深色模式下打不开的问题。

## 修复

- 深色模式下启动即崩溃：`onCreate` 里在 `setContentView` 之前调用 `getWindow().getInsetsController()`，此时 DecorView 还没 attach，部分 ROM 上直接抛 `NullPointerException`（`DecorView.getWindowInsetsController() on a null object reference`）
- 现在改为 `setContentView` 之后再取 `decorView.getWindowInsetsController()`，并整段包 try/catch；取不到时退回 `setSystemUiVisibility(0)`
- 窗口背景设置也一并包了 try/catch

## 说明

- 只影响界面启动路径，拦截逻辑未改动
- 1.6.0 / 1.6.1 的其它改动不变

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.6.1...v1.6.2
