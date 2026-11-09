感谢您使用 HyperPGuard。

这一版补上 libxposed API 102 的热重载支持：模块更新后，已经加载旧版模块的进程可以直接切换到新代码，不必逐个重启。

## 新增

- 实现 `XposedModuleInterface.onHotReloading`，返回 `true` 声明本模块允许热重载；返回前停掉模块自建的 Application 兜底线程
- 实现 `XposedModuleInterface.onHotReloaded`：卸载上一代 hook 句柄，按进程当前的 Application 重新读取开关并重新安装拦截

## 说明

- 框架不会在热重载后重放 `onModuleLoaded` / `onPackageReady`，拦截由 `onHotReloaded` 自行重装；热重载期间旧 hook 已卸载，新 hook 随即装上
- `module.prop` 保持 `autoHotReload=true`，作用域、默认开关与拦截规则未改动
- 模块更新后 LSPosed 的状态通知由「N 个进程热重载失败」变为「N 个进程已热重载」
- 首次安装本版本时，仍在运行旧版模块代码的进程会被记一次热重载失败；这些进程重新启动后即恢复正常

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.6.7...v1.6.8
