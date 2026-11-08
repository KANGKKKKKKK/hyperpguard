感谢您使用 HyperPGuard。

本次更新给安装页广告位补了方法层。

## 优化

- 「安装页广告位」从仅网络层改为网络层 + 方法层：按「static void、7 个参数、第 3 参为 Integer、末参是回调接口」定位请求入口并置空
- 定位只看方法结构，不依赖类名与方法名，组件升级后仍可命中；命中数写入日志 `packageinstaller ad method hooked=N`
- 安装器三项（AVL、活跃统计、广告位）均为网络层 + 方法层，各自独立开关

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.7...v1.5.8
