感谢您使用 HyperPGuard。

本次更新修复服务断开后自检一直显示未连接的问题。

## 修复

- LSPosed 服务断开后自动重连，最多重试五次；覆盖安装模块时服务端会重启一次，此前需要手动重开界面
- 自检在服务未连接时给出说明，并提示以 LSPosed 日志里的注入记录为准

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.5...v1.3.6
