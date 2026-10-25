#!/bin/bash
set -e

export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$HOME/Library/Android/sdk/build-tools/34.0.0:$HOME/Library/Android/sdk/platform-tools:$PATH

ANDROID_JAR=$HOME/Library/Android/sdk/platforms/android-34/android.jar
BUILD_TOOLS=$HOME/Library/Android/sdk/build-tools/34.0.0

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "🚀 COMPILANDO ANTIGRAVITY MOBILE PRO APK (v2.5.0)"
echo "=========================================================="

echo "=== [1/6] Limpando diretórios de build anteriores ==="
rm -rf build/classes build/dex build/gen build/compiled_res.zip build/app-res.apk build/app-with-dex.apk
mkdir -p build/classes build/dex build/gen dist

echo "=== [2/6] Compilando recursos com AAPT2 ==="
$BUILD_TOOLS/aapt2 compile --dir res -o build/compiled_res.zip

echo "=== [3/6] Vinculando APK e gerando R.java ==="
$BUILD_TOOLS/aapt2 link -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -o build/app-res.apk \
  -A assets \
  build/compiled_res.zip \
  --java build/gen

echo "=== [4/6] Compilando código Java com javac (Java 17) ==="
javac -cp "$ANDROID_JAR:build/gen" \
  -d build/classes \
  build/gen/com/mateuscelestino/antigravitymobile/R.java \
  src/com/mateuscelestino/antigravitymobile/KeepAliveService.java \
  src/com/mateuscelestino/antigravitymobile/SessionMigrator.java \
  src/com/mateuscelestino/antigravitymobile/MainActivity.java

echo "=== [5/6] Gerando Dex bytecode com D8 ==="
$BUILD_TOOLS/d8 --output build/dex --lib "$ANDROID_JAR" $(find build/classes -name "*.class")

echo "=== [6/6] Empacotando classes.dex, alinhando com zipalign e assinando ==="
cp build/app-res.apk build/app-with-dex.apk
cd build/dex
zip -u ../app-with-dex.apk classes.dex
cd ../..

$BUILD_TOOLS/zipalign -p -f -v 4 build/app-with-dex.apk dist/antigravity-mobile-pro-aligned.apk

$BUILD_TOOLS/apksigner sign --ks antigravity.keystore \
  --ks-key-alias antigravity \
  --ks-pass pass:antigravity2026 \
  --key-pass pass:antigravity2026 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out dist/antigravity-mobile-pro.apk \
  dist/antigravity-mobile-pro-aligned.apk

echo "=== [VERIFICAÇÃO DE ASSINATURA] ==="
$BUILD_TOOLS/apksigner verify --verbose dist/antigravity-mobile-pro.apk

echo "=========================================================="
echo "✅ SUCESSO! APK OFICIAL GERADO EM dist/antigravity-mobile-pro.apk"
echo "=========================================================="
ls -lh dist/antigravity-mobile-pro.apk
