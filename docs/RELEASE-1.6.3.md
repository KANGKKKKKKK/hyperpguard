感谢您使用 HyperPGuard。

这次修的是 1.6.2 暴露的几处界面与连接问题。

## 修复

- **深色模式启动崩溃**：`getInsetsController()` 在 `setContentView` 之前调用会拿到空 DecorView，改为之后再取并整段 try/catch
- **浅色模式状态栏看不见**：图标颜色设置被包在夜间分支里，浅色下图标仍是白色；现在深浅都按模式设置 `APPEARANCE_LIGHT_STATUS_BARS`
- **沉浸式**：Android 12+ 补上 `setDecorFitsSystemWindows(false)`，状态栏/导航栏透明才真正生效；顶部与底部按系统栏高度留白
- **LSPosed 服务偶发未连接**：服务未绑定时不再直接报错，而是把动作排队、重新注册监听，绑定成功后自动执行，4 秒仍未连上才提示
- 顶部「开关打开 = 拦截该行为」的蓝色色块改为灰色小字

## 新增

- 分组未进入 LSPosed 作用域时，卡片内显示「未在作用域，点此加入」，点一下直接申请；README 同步说明「覆盖升级后作用域不会自动包含新增组件」

## 说明

- 1.6.x 新增的组件是「广告组件（MSA）」与「系统诊断」，升级后请到 LSPosed → 模块 → HyperPGuard → 作用域 里确认
- 拦截逻辑与默认值未改动

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.6.2...v1.6.3
