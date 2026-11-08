感谢您使用 HyperPGuard。

本次合入三条功能请求，并补了界面深色模式。

## 新增

- **安全中心统计埋点**（默认开）：拦 `tracking.*.miui.com` 与 `sdkconfig.*.xiaomi.com` 共 10 条规则，覆盖 OneTrack 的 `/track/v4`、`/track/key_get` 与 `sdkconfig` 的 config / token 路径。host 是裸域名，按 CN / RU / INTL / IN / EU 五套区域各一条
- **MSA 广告组件**（`com.miui.systemAdSolution`，作用域 +2 个子项，默认开）：
  - 广告使用画像：判断「应用今天是否打开」「最后使用时间」的调用按少推广告一侧返回，不会因为查不到数据反而多推
  - 广告埋点：OneTrack 的 `track*` / `adTrack*` 方法直接返回空
  - 类名跨版本会漂移，按「修饰符 + 参数 + 返回类型 + 声明类所在包」组合判定，不写死类名
- **界面**：跟随系统深色模式，卡片圆角 18dp、分组标题 14sp + 60% 透明度、开关改为细轨道 + 小滑块

## 说明

- MSA 的域名层默认不拦：广告 SDK 请求失败会重试，方法层更干净；需要时用自定义规则补
- `scope.list` 新增 `com.miui.systemAdSolution`，覆盖安装后需要在 LSPosed 里确认该包在作用域内
- 拦截逻辑与默认值之外的部分未改动

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.15...v1.5.16
