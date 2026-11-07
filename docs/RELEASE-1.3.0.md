感谢您使用 HyperPGuard。

本次更新调整了设备标识的拦截口径：安全中心默认不再拦截，套餐与流量恢复正常。

## 优化

- 安全中心的设备标识改为单独开关，默认关闭（放行）：此前拦截 IMSI / ICCID / 手机号会让剩余流量、套餐信息、营业厅入口失效
- IMSI 改为「真实运营商前缀 + 固定 MSIN」的假值，ICCID 改为固定假值，IMEI / MEID / DeviceId 仍返回空值；其余组件的拦截口径不变

## 修复

- 安全中心能打开但流量数据为空、提示设置套餐
- 拦截覆盖到 `getSubscriberIdForSubscriber`、`getSimSerialNumberForSubscription` 等重载

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.15...v1.3.0
