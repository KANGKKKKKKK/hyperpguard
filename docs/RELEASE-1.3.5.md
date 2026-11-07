感谢您使用 HyperPGuard。

本次更新让环境自检能给出可判断的注入结果。

## 优化

- 自检新增「总开关」状态，关闭时所有进程都会跳过安装，这一点会直接显示出来
- 「已注入进程」改为读 LSPosed 的 modules 日志，不再依赖 logcat 环形缓冲，运行久了也能看到
- attach 日志改为读取全部日志缓冲，并为无记录的情况区分「缓冲已滚出」与「未注入」

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.4...v1.3.5
