感谢您使用 HyperPGuard。

本次更新给应用安装器补了方法层拦截。

## 优化

- AVL 上报入口改为方法层拦截：按「静态 4 参、第二参为 ApkInfo、返回 void」定位并置空，覆盖所有调用路径，不再只依赖 `api.installer.xiaomi.com/avl/upload` 这一条 URL
- 类名前缀变化时退到全量扫描，日志会打印 `packageinstaller avl method hooked=N`

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.4...v1.5.5
