感谢您使用 HyperPGuard。

本次更新补齐了设备标识的拦截缺口。

## 修复

- `miui.telephony.TelephonyManager` 与 `TelephonyManagerEx` 之前没有任何拦截，而运营商注册组件正是通过这两个类读取 IMEI、MEID、IMSI、ICCID
- AOSP `TelephonyManager` 只拦了无参方法，现补齐带 slot 参数的重载
- 新增 `android.net.wifi.WifiInfo#getMacAddress` 拦截，返回固定地址而非 null，避免调用方拼接时崩溃

## 说明

- 网络层与行为库层不变
- 设备注册（`zzhc.vnet.cn`）仍为默认关闭，开启会影响 VoLTE 与运营商功能

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.8...v1.2.9
