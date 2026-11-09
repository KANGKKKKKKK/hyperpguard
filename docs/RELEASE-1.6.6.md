感谢您使用 HyperPGuard。

修复安装页两处漏拦。

## 修复

- 5.4.x 安装器把云端参数改走 `/v3/cloud_config/query`，之前只拦 `/v2/`，导致「未查询到备案」等提示仍会出现；现在 v2 / v3 都覆盖，`preview-api.installer.xiaomi.com` 同步补齐
- `api.ad.xiaomi.com`、`monitor-check.ad.xiaomi.com`、`sdkconfig.ad.xiaomi.com` 三条域名规则原本被放在「路径特征拦截」开关分支里，关掉该开关后安装页广告就完全不受拦；现在域名规则无条件生效，只有 `/info/layout` 与缩略图路径仍受路径开关控制

## 说明

- 拦截范围与默认值未改动
- 安装器若在作用域外，卡片会显示「未在作用域，点此加入」

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.6.5...v1.6.6
