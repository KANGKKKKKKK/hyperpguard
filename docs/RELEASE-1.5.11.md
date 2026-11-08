感谢您使用 HyperPGuard。

本次更新修的是安全中心 13.x 上 XSOF 拦截整段失效的问题。

## 修复

- `stage xsof fail: NoClassDefFoundError: Ljavax/lang/model/element/AnnotationMirror`：安全中心把 AutoValue 的注解处理器打进了 APK，个别类的方法签名引用了 Android 运行时没有的 `javax.lang.model.*`，`getDeclaredMethods()` 解析签名时抛错并冒到 stage 兜底，后面的写库与评分 hook 就都没装上。现在按类逐个兜住异常，只跳过出错的那几个类，扫描继续
- 行为库写入口改成结构识别：按「SQLiteOpenHelper 子类 + 实现同包接口 + 一组 bean 参数写方法」定位，不再依赖写死的混淆名。安全中心 12.x 命中 `Ra.k`（16 个写方法），13.6.6 命中 `jo.k`（16 个）。旧版写死的 `Ra.k` 与 `Qa.*` 谓词在 13.6.6 上已经一个都命不中
- 评分出口同样改成结构识别：返回 float、参数为 XSOF 评分 bean 的方法。12.x 是 `Pa.b#j`，13.6.6 是 `ho.b#j`
- analytics 的方法扫描加同一层保护，避免单个类解析失败拖垮整轮扫描

## 说明

- 实测环境：安全中心 12.8.8-261028.0.1 与 13.6.6-261013.0.1，两版 provider / AIDL / 写库 / 评分四层全部命中，拦截实测通过
- 未改动任何开关与默认值

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.10...v1.5.11
