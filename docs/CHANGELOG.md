# 更新记录

较早版本的功能变更，按版本倒序排列。1.5 起的版本见 [Releases](https://github.com/vivibibi30/hyperpguard/releases) 与 `docs/RELEASE-<版本>.md`。

### 1.1.2

- 安全中心：新增「行为库写入拦截」（默认开），按 8 张表名拦 `SQLiteDatabase` 写入
- 系统安全组件：新增「静默录屏拦截」（默认开），`MediaProjection#createVirtualDisplay` 不再建立

### 1.1.1

- SystemHelper：新增「全断网」子项（默认开），该进程所有非本机请求全部拦截
- 小米统计：新增「全断网」子项（默认开）

### 1.1.0

- 新增隐私 API 层：应用列表 / 使用记录 / 设备标识 / 短信 / XMSF 代发广播
- 小米统计：域名拦截扩展为整域 + 备份域 + DoH + 签名扫描方法层
- 新增运营商服务分组：明文 CTEI 默认拦、设备注册默认关
- 安全中心子项从 3 个增至 8 个，其中 5 个默认关闭
- 修复组门 fallback 导致六段 hook 未安装

### 1.0.1

- 迁移到 libxposed API 102（`XposedModule` / `onPackageReady`）
- 作用域改为 `getScope` / `requestScope` / `removeScope`，不再改 `modules_config.db`
- 组开关按真实作用域回填；组关闭时子项置灰不可操作
- 「重启」「查看记录」的 root 提示移入设置卡片，启动不再自动 `su`
- 构建：固定签名并校验指纹，`META-INF` 打进 APK 根目录，CI 使用 build-tools 35

### 1.0.0

首个版本：四环节拦截、按应用分组、传统 Xposed API 82。
