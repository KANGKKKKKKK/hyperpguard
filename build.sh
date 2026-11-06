#!/bin/sh
# PrivacyGuard build script
# 依赖：JDK 11+ / android.jar / d8 / aapt2 / zipalign / apksigner
set -e

ROOT=$(cd "$(dirname "$0")" && pwd)
cd "$ROOT"

: "${ANDROID_JAR:?请设置 ANDROID_JAR 指向平台 android.jar}"
: "${D8:?请设置 D8 指向 build-tools 中的 d8}"
AAPT2=${AAPT2:-aapt2}
ZIPALIGN=${ZIPALIGN:-zipalign}
APKSIGNER=${APKSIGNER:-apksigner}
KS=${KEYSTORE:-$ROOT/debug.keystore}
API=$ROOT/libs/api-82.jar

if [ ! -f "$KS" ]; then
  echo "未找到 keystore，生成 debug 密钥：$KS"
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

rm -rf build PrivacyGuard.apk
mkdir -p build/classes build/dex build/gen

echo "[1/6] javac"
javac --release 11 -encoding UTF-8 -nowarn \
  -cp "$ANDROID_JAR:$API" -d build/classes \
  $(find src -name '*.java')

echo "[2/6] d8"
"$D8" --release --min-api 29 --lib "$ANDROID_JAR" --output build/dex \
  $(find build/classes -name '*.class')

echo "[3/6] aapt2 compile"
"$AAPT2" compile --dir res -o build/gen/res.zip

echo "[4/6] aapt2 link"
"$AAPT2" link -o build/app-unsigned.apk \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -R build/gen/res.zip \
  -A assets \
  --min-sdk-version 29 --target-sdk-version 34 \
  --auto-add-overlay

echo "[5/6] add dex"
cp build/dex/classes.dex build/classes.dex
(cd build && zip -q app-unsigned.apk classes.dex)
"$ZIPALIGN" -f 4 build/app-unsigned.apk build/app-aligned.apk

echo "[6/6] sign"
"$APKSIGNER" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out PrivacyGuard.apk build/app-aligned.apk

echo "OK -> $ROOT/PrivacyGuard.apk"
ls -la PrivacyGuard.apk
