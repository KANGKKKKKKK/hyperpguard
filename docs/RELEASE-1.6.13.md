感谢您使用 HyperPGuard。

这一版把 1.6.12 拆得过细的输入法开关合并回来：影响同一件事的拦截归为一类，开关描述里写明可能的后果。

## 调整

- 搜狗输入法：`kpi2` 与 `instantpb` 合回「统计上报」一个开关，与「剪贴板采集」共两项
- 小米输入法：OneTrack 方法层与 tracking/sdkconfig 域名合回「统计埋点」一个开关
- 百度输入法：统计 SDK 与语音 ASR 上传保持两项
- 讯飞输入法：统计 SDK 一项
- 每个开关的描述补上「后果」：只停统计 / 关闭后剪贴板历史可能不可用 / 语音仍可用但不再上传 ASR 数据

## 说明

- 默认值不变：百度语音上传、搜狗剪贴板采集默认关闭，其余默认开启
- 拦截规则未改动，只调整开关划分与文案
- 1.6.12 的 `block_sogou_kpi2`、`block_sogou_instant`、`block_mitype_hosts` 三个键不再使用，回退到 `block_sogou_pingback` 与 `block_mitype_track`

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.6.12...v1.6.13
