# HyperPGuard 1.2.1

## 变更（对比 1.2.0）

| 项 | 1.2.0 | 1.2.1 |
|---|---|---|
| 批量操作 | 全部启用 / 全部停用 | 同一行三键：全部启用 / 全部停用 / 全部重启 |
| 全局卡片 | 查看记录 / 全部重启 / 分享日志 | 查看记录 / 分享日志 |
| 其余 | — | 不变 |

「全部重启」对每个已安装的目标应用执行 `am force-stop`，需要 root。

## 校验

```
签名证书  SHA-256 cacdf86d34ff7f7de0ed99467364ff5fc9d898f6c61d2202a02a34050d8cdcd3
```

## 参考与致谢

- **HyperCeiler**（GPL-3.0，Sevtinge）
- **XSOF Privacy**（yunyizea）
- **Hyper 隐私卫士**（`io.github.hyperprivacyguard`）
- **HyperOS4-XSOF-Analysis**（FAQQAQ）
- **OSAKOo**（酷安）
- **libxposed API**（Apache-2.0）、**LSPosed**

代码独立编写，未复制、未反编译复用上述项目的实现。

## 免责声明

仅供在本人拥有完全控制权的设备上学习研究使用，不做商业分发，不得移除声明后转载。完整条款见 [DISCLAIMER.txt](https://github.com/vivibibi30/hyperpguard/blob/main/DISCLAIMER.txt)。GPL-3.0。
