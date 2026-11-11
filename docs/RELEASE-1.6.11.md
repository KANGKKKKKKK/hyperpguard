感谢您使用 HyperPGuard。

这一版加入三组新拦截：MSA 开屏广告、网络管理服务与 AI 内容识别的 OneTrack 埋点、四个预装输入法的统计与搜狗剪贴板采集。规则与实现由 @KANGKKKKKKK 提交（#18、#19），本地合并时补了两处调整。

## 新增

- MSA 开屏广告：`requestSplashScreen` 系列返回空 Intent，应用自身的启动页不受影响
- 网络管理服务（`com.miui.wmsvc`）：OneTrack 埋点 + `tracking.*.miui.com` / `sdkconfig.*.xiaomi.com`
- AI 内容识别（`com.xiaomi.aicr`）：OneTrack 埋点 + 同上域名，本地识别与推理不受影响
- 搜狗输入法：`kpi2` / `instantpb` 统计域名，以及 `ClipboardLogHelper` 的剪贴板内容记录
- 小米输入法：OneTrack 埋点（自研击键统计类名混淆且无稳定域名，未覆盖）
- 百度输入法：`com.baidu.stats` 统计 SDK、`com.baidu.speech.utils.analysis` 的 ASR 采集上传
- 讯飞输入法：`com.iflytek.statssdk` 统计 SDK
- 新增公共 `TrackHooks`，MSA / wmsvc / aicr / 输入法共用同一套 OneTrack 收口

## 调整

- 输入法的埋点拦截改为「整包 void 方法，排除 `init` / `set` / `start` / `register` 等控制方法」，避免统计 SDK 带坏状态运行
- 百度「语音采集上传」默认关闭：影响语音链路，需要时手动开
- 组开关点击方向修复并入本版（1.6.10 已单独发过，此处只影响从 1.6.9 及更早升级的用户）

## 说明

- 作用域由 13 个包增加到 19 个；新包需要在 LSPosed 管理器勾选，首次点开对应组开关时 LSPosed 会发「作用域请求」通知
- 拦截规则只停埋点与上报，不影响输入、候选、词库与语音输入本身的可用性
- 免责声明、界面与授权逻辑未改动

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.6.10...v1.6.11
