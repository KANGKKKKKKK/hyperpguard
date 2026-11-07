感谢您使用 HyperPGuard。

本次更新按国行预装应用隐私研究补齐了数据来源侧的拦截。

## 新增

- 设备标识补齐：Android ID、硬件序列号（`Build.getSerial`）、手机号（`getLine1Number`），此前只有 IMEI / MEID / IMSI / ICCID / MAC
- 新增「敏感数据」分组，默认关闭：
  - 定位（`LocationManager` 查询与注册）
  - 基站信息（`getCellLocation` / `getAllCellInfo`）
  - Wi-Fi 扫描与 SSID / BSSID
  - 通讯录 / 通话记录 / 短信的 `ContentResolver` 查询

## 说明

- 设备标识默认开启，返回空值或固定值，不影响功能
- 敏感数据默认关闭；开启后定位、信号显示、来电识别等功能会受影响，按需打开

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.13...v1.2.14
