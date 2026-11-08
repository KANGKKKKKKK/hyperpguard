感谢您使用 HyperPGuard。

本次更新给安装页广告位补了方法层。

## 优化

- 「安装页广告位」从仅网络层改为网络层 + 方法层：按「static void、7 个参数、第 3 参为 Integer、末参是回调接口」定位请求入口并置空
- 定位只看方法结构，不依赖类名与方法名，组件升级后仍可命中；命中数写入日志 `packageinstaller ad method hooked=N`
- 安装页广告补拦三个域名：`api.ad.xiaomi.com`、`monitor-check.ad.xiaomi.com`、`sdkconfig.ad.xiaomi.com`
- 新增「设备标识（OAID）」：hook MSA SDK 的 OAID 获取并返回空串，只影响广告归因
- 安装器四项（AVL、活跃统计、广告位、OAID）各自独立开关

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.7...v1.5.8
