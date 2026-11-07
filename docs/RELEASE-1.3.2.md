感谢您使用 HyperPGuard。

本次更新新增系统诊断组件（MiSight）的上传拦截。

## 新增

- 新增 `com.miui.misightservice` 覆盖，三条规则默认开启：
  - 日志上传：`feedback.miui.com` 的 `/misight/upload/`
  - 售后检测：`port.sec.miui.com` 的 `/mqsas/kdxFileUpload`
  - 客服诊断：`chat.kefu.mi.com` 的 `/openapi/misight/diagnostic/`

## 说明

- 这三条链路只在用户主动提交反馈、售后远程检测或在线客服诊断时触发
- 本地数据库里的记录不等于已经上传；关闭对应规则会让上述功能拿不到日志包

## 致谢

- [misight-viewer](https://github.com/Artriai/misight-viewer)（作者 [Artriai](https://github.com/Artriai)）— MiSight 数据库字段与事件分类

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.1...v1.3.2
