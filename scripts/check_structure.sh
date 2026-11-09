#!/bin/sh
# 静态一致性自查；默认只报告，--strict 时发现不一致会返回非零
cd "$(dirname "$0")/.." || exit 1
exec python3 scripts/check_structure.py "$@"
