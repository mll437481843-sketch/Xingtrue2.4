#!/bin/sh
# On-device (or any Linux) build: aapt2 -> javac -> d8 -> zipalign -> apksig.
# Requires JDK 17; set TC to a directory containing prefix/bin/{aapt2,zipalign},
# platform/android-36/android.jar and lib/{r8,apksig}.jar.
set -e
BASE=${BASE:-$(cd "$(dirname "$0")" && pwd)}
TC=${TC:?set TC to your toolchain dir}
KEYSTORE=${KEYSTORE:-$BASE/keystore.jks}
KEYSTORE_PASS=${KEYSTORE_PASS:-changeit}
KEY_ALIAS=${KEY_ALIAS:-bilisave}

export LD_LIBRARY_PATH="$TC/prefix/lib"
JAVA="$TC/prefix/lib/jvm/java-17-openjdk/bin/java"
JAVAC="$TC/prefix/lib/jvm/java-17-openjdk/bin/javac"
JAR="$TC/prefix/lib/jvm/java-17-openjdk/bin/jar"
AAPT2="$TC/prefix/bin/aapt2"
ZIPALIGN="$TC/prefix/bin/zipalign"
PLATFORM="$TC/platform/android-36/android.jar"
R8="$TC/lib/r8.jar"
APKSIG="$TC/lib/apksig.jar"

cd "$BASE"
rm -rf build/flat build/gen build/classes build/dex build/sign build/unsigned.apk build/aligned.apk build/aligned2.apk
mkdir -p build/flat build/gen build/classes build/dex build/sign

echo "[1/6] aapt2 compile"
"$AAPT2" compile --dir res -o build/flat/res.zip

echo "[2/6] aapt2 link"
"$AAPT2" link -o build/unsigned.apk -I "$PLATFORM" --manifest AndroidManifest.xml \
  -R build/flat/res.zip --java build/gen --min-sdk-version 26 --target-sdk-version 35 \
  --version-code 2 --version-name 1.1 --auto-add-overlay

echo "[3/6] javac"
find src build/gen -name "*.java" > build/sources.txt
"$JAVAC" -source 11 -target 11 -nowarn -classpath "$PLATFORM" -d build/classes @build/sources.txt
"$JAVAC" -nowarn -classpath "$APKSIG" -d build/sign tools/SignApk.java

echo "[4/6] d8"
"$JAVA" -cp "$R8" com.android.tools.r8.D8 --release --min-api 26 --lib "$PLATFORM" \
  --output build/dex $(find build/classes -name '*.class' | tr '\n' ' ')

echo "[5/6] package + align"
cp build/unsigned.apk build/aligned.apk
"$JAR" uf build/aligned.apk -C build/dex classes.dex
"$ZIPALIGN" -f 4 build/aligned.apk build/aligned2.apk

echo "[6/6] sign"
"$JAVA" -cp build/sign:"$APKSIG" SignApk build/aligned2.apk build/BiliSave-release.apk \
  "$KEYSTORE" "$KEY_ALIAS" "$KEYSTORE_PASS"
ls -la build/BiliSave-release.apk

