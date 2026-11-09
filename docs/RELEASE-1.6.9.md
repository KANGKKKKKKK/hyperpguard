感谢您使用 HyperPGuard。

这一版重做免责声明的排版：正文不再依赖手工折行，标题、版本行、章节与列表项各自套用样式，首启摘要也不再按字符截断。

## 修复

- `assets/DISCLAIMER.txt` 按段落重排，一段一行，去掉两条 ASCII 分隔线，编号统一为 `1) `，第九节重复的 `5)` 改为 `6)`
- 完整条款改为 `SpannableStringBuilder` 渲染：标题放大加粗、版本行缩小置灰、章节标题加粗、列表项用 `LeadingMarginSpan` 悬挂缩进（折行后的行同样缩进）
- 正文 11sp 调整为 13sp，行距 1.25，左右留白改用独立常量，不再复用免责声明版本号
- 首启摘要改用新增的 `assets/DISCLAIMER_BRIEF.txt`，不再对正文做 `substring` 截断
- 英文界面取 `English Summary` 段改为按标题定位，不再依赖分隔线

## 说明

- 声明正文的措辞与条款含义未改动，只调整分段与展示方式
- 免责声明确认状态未重置，已确认过的用户不会重新弹窗
- 拦截逻辑与默认值未改动

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.6.8...v1.6.9
