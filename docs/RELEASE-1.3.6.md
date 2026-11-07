感谢您使用 HyperPGuard。

本次更新让 SystemHelper 在组件升级后仍能定位到数据出口。

## 优化

- 固定类名对不上时按方法签名兜底：`(String, byte[], byte[])` 返回 boolean 的方法作为数据出口，`(String)` 返回 void 的方法作为写库入口
- 兜底也落空时，把候选类名或类名样本写进日志，便于按组件版本补充固定类名
- dispatch 失败时打印逐行堆栈，便于定位触发的类加载
- 自检新增「安装失败记录」，汇总安装阶段的失败行

## 说明

- 只影响方法层，网络层与隐私 API 层不依赖这些类名
- 日志里出现 `systemhelper upload fallback=0 probe=[...]` 时，把那一行贴到 issue 即可

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.3.5...v1.3.6
