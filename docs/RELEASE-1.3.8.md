感谢您使用 HyperPGuard。

本次更新修复日志入口在 Android 13 及以上一直为空的问题。

## 修复

- logcat 的 tag 从 `I/HyperPGuard:` 变成带进程号的 `I/HyperPGuard( 1234):`，原来的过滤条件匹配不到，导致「查看日志」始终为空
- 过滤条件改为同时兼容两种格式；自检的「安装失败记录」与 attach 记录同步修正

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.7...v1.3.8
