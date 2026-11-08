感谢您使用 HyperPGuard。

本次更新把「自定义规则」扩展了一档，并修掉一处规则装载不一致。

## 新增

- 自定义规则支持 **方法级** 写法 `类名#方法`，例如 `jo.k#U0`、`com.xiaomi.security.xsof.XSOFProvider#query`。命中的方法不再执行原逻辑，直接返回默认值：`void` 与引用类型返回 `null`，`boolean` 返回 `false`，数值返回 `0`。方法名写 `*` 表示整类全部方法
- 方法级规则用于内置 hook 因类名变化失配时应急：某版安全中心改了混淆名导致 `xsof db write hooked=0`，可以先补一条规则顶住，不必等模块更新

## 修复

- `guardprovider`、`misightservice`、`registration`、`packageinstaller` 四个包在内置规则为空时会跳过网络层，导致自定义规则在这几个包上不生效；现在统一无条件装载
- 方法级规则在进程内拦截分发之后安装，不受单个应用分组开关影响；总开关关闭时仍不安装

## 说明

- 规则解析与匹配只读取你自己填的内容，未填则完全不安装，默认行为与之前一致
- 写法、匹配细节、适用范围与限制见 [自定义规则使用说明](https://github.com/vivibibi30/hyperpguard/blob/main/docs/CUSTOM-RULES.md)

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.5.11...v1.5.12
