# 给 Copilot 的说明

仓库的约定写在根目录 [AGENTS.md](../AGENTS.md)，结构说明在 [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)。

简单说：改完跑 `sh build.sh` 和 `sh scripts/check_structure.sh`；新增组件时同步 `scope.list`、`Config`、`HookEntry`、`GROUPS`、`I18n`、`docs/RULES.md`；不要引入新的第三方依赖，也不要主动改版本号。
