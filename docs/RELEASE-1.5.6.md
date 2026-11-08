感谢您使用 HyperPGuard。

本次更新补上 okhttp 自定义 DNS 的拦截。

## 优化

- 目标组件普遍内置 okhttp。默认 DNS 实现走 `InetAddress.getAllByName`，原本已覆盖；使用 DoH 或内置 IP 列表的 `Dns` 实现会绕过，现在按接口实现类补拦一层

## 说明

- 覆盖的是 Java 层；native 网络栈仍不在范围内
- 日志会打印 `<组件> okhttp dns hooked=N`

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.5...v1.5.6
