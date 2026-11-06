#!/bin/sh
# 发布 GitHub Release（含 APK 附件），只用 GitHub REST API，不需要 gh CLI
#
# 用法：
#   GITHUB_TOKEN=ghp_xxx sh scripts/release.sh [APK 路径]
#
# 可选环境变量：REPO / TAG / NAME / BODY
set -e

TOKEN=${GITHUB_TOKEN:?请提供 GITHUB_TOKEN（带 repo 权限的 PAT）}
REPO=${REPO:-vivibibi30/pguard}
TAG=${TAG:-v1.1.0}
NAME=${NAME:-PrivacyGuard 1.1.0}
BODY=${BODY:-docs/RELEASE-1.1.0.md}
APK=${1:-PrivacyGuard.apk}
API=https://api.github.com
AUTH="Authorization: Bearer $TOKEN"

[ -f "$APK" ] || { echo "找不到 APK：$APK" >&2; exit 1; }
[ -f "$BODY" ] || { echo "找不到发布说明：$BODY" >&2; exit 1; }

echo "[1/4] 检查远端 tag $TAG"
code=$(curl -sS -o /tmp/pg_tag.json -w '%{http_code}' -H "$AUTH" \
  "$API/repos/$REPO/git/ref/tags/$TAG")
if [ "$code" != "200" ]; then
  echo "远端没有 tag $TAG（HTTP $code），先推 tag：git push origin $TAG" >&2
  exit 1
fi

echo "[2/4] 生成 payload"
python3 - "$TAG" "$NAME" "$BODY" > /tmp/pg_release.json <<'PY'
import json, sys
tag, name, body_path = sys.argv[1], sys.argv[2], sys.argv[3]
body = open(body_path, encoding='utf-8').read()
print(json.dumps({
    "tag_name": tag,
    "name": name,
    "body": body,
    "draft": False,
    "prerelease": False,
}, ensure_ascii=False))
PY

echo "[3/4] 创建 release"
code=$(curl -sS -o /tmp/pg_rel.json -w '%{http_code}' -H "$AUTH" \
  -H "Accept: application/vnd.github+json" -X POST \
  "$API/repos/$REPO/releases" -d @/tmp/pg_release.json)
case "$code" in
  201)
    REL_ID=$(python3 -c 'import json;print(json.load(open("/tmp/pg_rel.json"))["id"])')
    ;;
  422)
    REL_ID=$(curl -sS -H "$AUTH" "$API/repos/$REPO/releases/tags/$TAG" \
      | python3 -c 'import json,sys;print(json.load(sys.stdin)["id"])')
    echo "     release 已存在（id=$REL_ID），更新正文"
    curl -sS -o /dev/null -H "$AUTH" -H "Accept: application/vnd.github+json" \
      -X PATCH "$API/repos/$REPO/releases/$REL_ID" -d @/tmp/pg_release.json
    ;;
  *)
    echo "建 release 失败 HTTP $code" >&2
    cat /tmp/pg_rel.json >&2
    exit 1
    ;;
esac

echo "[4/4] 上传 $(basename "$APK")"
UP="$API/repos/$REPO/releases/$REL_ID/assets?name=$(basename "$APK")"
code=$(curl -sS -o /tmp/pg_asset.json -w '%{http_code}' -H "$AUTH" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @"$APK" "$UP")
if [ "$code" != "201" ]; then
  echo "上传失败 HTTP $code" >&2
  cat /tmp/pg_asset.json >&2
  exit 1
fi

python3 - <<'PY'
import json
d = json.load(open('/tmp/pg_asset.json'))
print("附件：", d["browser_download_url"])
print("大小：", d["size"], "字节")
PY
echo "完成：https://github.com/vivibibi30/pguard/releases/tag/v1.1.0"
