# PrivacyGuard 1.1.2

[![Build APK](https://github.com/vivibibi30/pguard/actions/workflows/build.yml/badge.svg)](https://github.com/vivibibi30/pguard/actions/workflows/build.yml)

## 变更（对比 1.1.1）

| 组件 | 1.1.1 | 1.1.2 |
|---|---|---|
| 安全中心 | XSOF 写库按 `Ra.k` 的 27 个方法拦截（类名随版本变化） | 新增「行为库写入拦截」子项（默认开）：hook `SQLiteDatabase` 的 `insert` / `insertOrThrow` / `insertWithOnConflict` / `update` / `delete` / `execSQL`，按 `screenshot` / `screen_share` / `call_forwarding` / `airplane_mode` / `fraud_phone` / `voip_record` / `fraud_url` / `install_app` 八张表过滤 |
| 系统安全组件 | 只拦 `detection-aigc.engine.miui.com` 上传 | 新增「静默录屏拦截」子项（默认开）：hook `MediaProjection#createVirtualDisplay`，录制不建立 |
| 其余组件 | — | 不变 |

## 校验

```
签名证书  SHA-256 cacdf86d34ff7f7de0ed99467364ff5fc9d898f6c61d2202a02a34050d8cdcd3
```

附件由 CI 构建，字节哈希以 Release 附件页显示为准；证书指纹固定。

## 参考与致谢

- **HyperCeiler**（GPL-3.0，Sevtinge）
- **XSOF Privacy**（yunyizea）
- **Hyper 隐私卫士**（`io.github.hyperprivacyguard`）
- **HyperOS4-XSOF-Analysis**（FAQQAQ）
- **OSAKOo**（酷安）— [HyperOS 行为数据库与静默录屏的复核](https://www.coolapk.com/feed/74882729?s=MzNlYzk4NWUxNWZhMjRnNmFlZTFjMDJ6a1664)：本版的行为库写入拦截与静默录屏拦截据此补充
- **libxposed API**（Apache-2.0）、**LSPosed**

代码独立编写，未复制、未反编译复用上述项目的实现。

## 免责声明

仅供在本人拥有完全控制权的设备上学习研究使用，不做商业分发，不得移除声明后转载。完整条款见 [DISCLAIMER.txt](https://github.com/vivibibi30/pguard/blob/main/DISCLAIMER.txt)。GPL-3.0。
