#!/usr/bin/env python3
"""静态一致性自查：只报告，不拦截构建（加 --strict 才返回非零）。"""
import re, sys, pathlib

root = pathlib.Path(__file__).resolve().parent.parent
strict = '--strict' in sys.argv
problems = []
notes = []

def read(p):
    return (root / p).read_text(encoding='utf-8')

cfg = read('src/io/github/vivibibi30/hyperpguard/Config.java')
manifest = read('AndroidManifest.xml')
main = read('src/io/github/vivibibi30/hyperpguard/MainActivity.java')
scope = read('META-INF/xposed/scope.list')

# 1) 版本号一致
ver = re.search(r'VERSION\s*=\s*"([^"]+)"', cfg)
man_ver = re.search(r'versionName="([^"]+)"', manifest)
if ver and man_ver and ver.group(1) != man_ver.group(1):
    problems.append(f'Config.VERSION={ver.group(1)} 与 AndroidManifest versionName={man_ver.group(1)} 不一致')

# 2) 免责声明两份一致
a = read('DISCLAIMER.txt')
b = read('assets/DISCLAIMER.txt') if (root / 'assets/DISCLAIMER.txt').exists() else ''
if a != b:
    problems.append('DISCLAIMER.txt 与 assets/DISCLAIMER.txt 不一致（应用内显示的是 assets 那份）')

# 3) scope.list 里的包都能在界面里找到
for pkg in [l.strip() for l in scope.splitlines() if l.strip()]:
    if f'"{pkg}"' not in main:
        problems.append(f'scope.list 里的 {pkg} 在 MainActivity 的 GROUPS 中没有对应条目')

# 4) 分组开关都在界面里用到
groups = re.findall(r'String (K_G_[A-Z0-9_]+)\s*=\s*"([^"]+)"', cfg)
for name, key in groups:
    if f'Config.{name}' not in main:
        problems.append(f'Config.{name}（{key}）没有在 MainActivity 中使用')

# 5) scope.list 内容提示
pkgs = [l for l in scope.splitlines() if l.strip()]
notes.append(f'scope.list 共 {len(pkgs)} 个包，Config 分组 {len(groups)} 个')

# 6) 默认值只有一个来源：Config.DEFAULTS
#
# 默认值曾经分两处写：MainActivity 的 Item 写一份，HookEntry 的 Config.on(key, 默认值)
# 写一份。1.6.12 把搜狗剪贴板的界面默认值改成关闭时漏改 hook 侧，界面显示「关」而
# 实际仍在拦截。现在两边都取 Config.def(key)，这里守住「不许再在调用点写字面量」。
defaults = dict(re.findall(r'DEFAULTS\.put\((K_[A-Z0-9_]+),\s*(true|false)\)', cfg))
src_dir = root / 'src' / 'io' / 'github' / 'vivibibi30' / 'hyperpguard'
sources = {p.name: p.read_text(encoding='utf-8') for p in src_dir.glob('*.java')}

literal_on = re.compile(r'Config\.on\((?:Config\.)?(K_[A-Z0-9_]+),\s*(?:true|false)\)')
literal_item = re.compile(r'new Item\([^;]*?Config\.(K_[A-Z0-9_]+),\s*(?:true|false)\)')
for name, text in sources.items():
    hits = [m.group(1) for m in literal_on.finditer(text)]
    if hits:
        problems.append(f'{name} 有 {len(hits)} 处 Config.on(key, true/false) 写了字面默认值'
                        f'（如 {hits[0]}）：改为 Config.on(key)，默认值登记到 Config.DEFAULTS')
    hits = [m.group(1) for m in literal_item.finditer(text)]
    if hits:
        problems.append(f'{name} 有 {len(hits)} 处 new Item(..., key, true/false) 写了字面默认值'
                        f'（如 {hits[0]}）：去掉该参数，Item 内部取 Config.def(key)')

referenced = set()
for text in sources.values():
    referenced.update(re.findall(r'Config\.(?:on|def)\(Config\.(K_[A-Z0-9_]+)\)', text))
referenced.update(re.findall(r'new Item\([^;]*?Config\.(K_[A-Z0-9_]+)\)', main))

for key in sorted(referenced - set(defaults)):
    problems.append(f'{key} 在用但没登记默认值（Config.DEFAULTS），未登记时按 false 处理')
for key in sorted(set(defaults) - referenced):
    problems.append(f'{key} 登记了默认值但没有任何地方使用')

notes.append(f'默认值表 {len(defaults)} 项')

# 7) I18n 文案键不重复
i18n = read('src/io/github/vivibibi30/hyperpguard/I18n.java')
seen_keys = {}
for m in re.finditer(r'EN\.put\("((?:[^"\\]|\\.)*)"', i18n):
    seen_keys[m.group(1)] = seen_keys.get(m.group(1), 0) + 1
for key, count in sorted(seen_keys.items()):
    if count > 1:
        problems.append(f'I18n 里「{key}」重复登记了 {count} 次，后者会覆盖前者')

print('结构自查：')
for n in notes:
    print('  ·', n)
if problems:
    for p in problems:
        print('  !', p)
    print(f'\n发现 {len(problems)} 处可能需要确认的地方。')
    if strict:
        sys.exit(1)
else:
    print('  ✓ 没有发现不一致')
