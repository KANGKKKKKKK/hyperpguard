感谢您使用 HyperPGuard。

本次更新修复了整进程断网的内网判断漏洞。

## 修复

- `isLocal()` 之前只做字符串前缀判断，`10.evil.com`、`192.168.evil.com`、`172.16.evil.com` 会被当成内网而放行，绕过 SystemHelper 与小米统计的整进程断网
- 现在只有合法的 IP 字面量才按网段判断，域名一律按外网处理
- 私有网段补全：172.16.0.0/12（原来只有 172.16/16）、169.254.0.0/16、IPv6 的 `::1`、`fe80::/10`、`fc00::/7`

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.2.11...v1.2.12
