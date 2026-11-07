感谢您使用 HyperPGuard。

本次更新加了行为库状态，用来验证拦截是否真的生效。

## 新增

- 「行为库状态」：读安全中心 `fraud_detection_data.db` 的 `sqlite_sequence`，按表显示累计写入次数
- 刷新后对增长的表标注 `(+N)`；拦截生效时这些数字应当保持不变
- 只读计数，不读通话号码、截图路径等记录内容；复制到应用私有目录，读完即删

## 说明

- 需要 root；读不到时会提示原因
- 数字来自 `sqlite_sequence`，只增不减，适合做前后对比

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.9...v1.4.0
