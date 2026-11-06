# 发布到 LSPosed 仓库

## 首次

1. 打开 <https://modules.lsposed.org/submission>，GitHub 登录后填 `https://github.com/vivibibi30/hyperpguard`
2. 审核通过后会出现分发仓库 `Xposed-Modules-Repo/io.github.vivibibi30.hyperpguard`
3. 把 `dist/` 下的 `SUMMARY`、`SOURCE_URL`、`README.md` 复制到该仓库根目录并推送

```sh
git clone https://github.com/Xposed-Modules-Repo/io.github.vivibibi30.hyperpguard
cp dist/SUMMARY dist/SOURCE_URL dist/README.md io.github.vivibibi30.hyperpguard/
cd io.github.vivibibi30.hyperpguard && git add -A && git commit -m "init" && git push
```

## 每次发版

```sh
# 源码仓库
sh build.sh
git add -A && git commit -m "..."
git tag v1.2.0 && git push origin main v1.2.0

# 分发仓库（tag 自动生成为 <versionCode>-<versionName>，例如 5-1.2.0）
sh scripts/publish-lsposed.sh
```

`publish-lsposed.sh` 从 APK 读取 `versionCode` 与 `versionName`，在分发仓库创建对应 tag 的 Release 并上传 APK，正文取 `docs/RELEASE-<versionName>.md`。
