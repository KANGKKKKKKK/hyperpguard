感谢您使用 HyperPGuard。

这次把设置界面按系统设置的度量重做了一遍，功能逻辑没动。

## 界面

- 行标题 17sp、副标题 14sp，副标题单行并在末尾省略；行高对齐系统 `miuix_preference_item_min_height` 的 56dp
- 分组标题改为小字灰色（14sp、高 35dp），放在卡片外
- 行间分隔线 0.75dp，左侧内缩 16dp，与系统列表分隔线一致
- 卡片圆角 20dp、间距 12dp、页面边距 16dp
- 开关改为 MIUI 的 `SlidingSwitchCompat` 规格：轨道 49×28dp、滑块 20dp、左右与上下各留 4dp，不再被主题拉伸
- 深色模式跟随系统，页面底色纯黑，卡片 `#1C1C1E`、分隔线 `#2C2C2E`

## 说明

- 以上数值取自设备上 `Settings.apk` 的 `miuix_*` 资源，不是估算
- 版本号 1.6.0，versionCode 51；50 之后的预览构建没有发布，正式号仍按 +1
- 功能与默认值未变：MSA 广告组件、安全中心统计埋点、XSOF 四层拦截等沿用 1.5.16

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.16...v1.6.0
