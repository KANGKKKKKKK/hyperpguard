感谢您使用 HyperPGuard。

本次更新更换了应用图标，并让开关在本地数据被清后仍能显示实际值。

## 新增

- 应用图标：蓝底白色禁止上传标记，五档尺寸（48–192）合计 13 KB

## 修复

- 本地配置被清空时，设置页的开关回落到 `Settings.Global` 里的实际值，不再显示成默认值
- 本地配置为空时不再写入 `Settings.Global`，避免把已有的用户设置冲成默认

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.7...v1.2.8
