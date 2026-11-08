感谢您使用 HyperPGuard。

本次更新给应用安装器补了第二处方法层拦截。

## 优化

- 「安装活跃统计」从仅网络层改为网络层 + 方法层：hook `AppActiveStatService` 内的 `void(List)` 入口，不再只依赖 `/statistics/appactive` 这一条 URL
- 安装器三个子项各自说明层次与影响：AVL 与活跃统计是网络层加方法层，广告位是网络层

## 说明

- 三个子项对应三个独立开关，互不影响
- 广告位仍只有网络层：它的接口返回 Retrofit 的 `Call`，置空会让调用方 NPE，因此选择不动接口本身

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.6...v1.5.7
