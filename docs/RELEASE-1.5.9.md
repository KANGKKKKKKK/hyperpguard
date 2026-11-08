感谢您使用 HyperPGuard。

本次更新把安装器的广告与备案提示一并处理。

## 优化

- 安装页推荐位补拦图片 CDN：`t1.a.market.xiaomi.com`、`t2.a.market.xiaomi.com` 的 `/thumbnail/` 路径
- 广告缓存读取改为返回空：hook 返回 `AdModel`、单参 `String` 的方法，推荐位不再渲染，已有缓存也不再显示
- 新增「云端配置下发」：拦截 `/v2/cloud_config/query`，这是「未查询到 ICP 备案」提示的来源；关闭后按内置默认值展示

## 说明

- 安装器五个子项各自独立开关：AVL 上报、活跃统计、广告位、设备标识（OAID）、云端配置下发
- 云端参数被拦后，按钮文案与安全模式提示会走内置默认值

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.8...v1.5.9
