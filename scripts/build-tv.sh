#!/usr/bin/env bash
# Build a self-contained, signed TV test APK; JAVA_HOME and ANDROID_HOME must be set.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
tv_arch="${TV_ARCH:-armeabi-v7a}"
tv_key="${TV_KEYSTORE:-$PWD/android/app/tv-test.keystore}"
tv_alias="${TV_KEY_ALIAS:-lx-tv-test}"
tv_pass="${TV_KEY_PASSWORD:-lx-tv-test}"
if [[ ! -f "$tv_key" ]]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$tv_key" \
    -storepass "$tv_pass" -keypass "$tv_pass" -alias "$tv_alias" \
    -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=LX Music TV Local Test'
fi
if [[ ! -d node_modules ]]; then npm ci --no-audit --no-fund; fi
cd android
./gradlew assembleTvRelease -PtvTestBuild \
  "-PreactNativeArchitectures=$tv_arch" \
  "-PMYAPP_UPLOAD_STORE_FILE=$tv_key" \
  "-PMYAPP_UPLOAD_KEY_ALIAS=$tv_alias" \
  "-PMYAPP_UPLOAD_STORE_PASSWORD=$tv_pass" \
  "-PMYAPP_UPLOAD_KEY_PASSWORD=$tv_pass" \
  --max-workers=2 "$@"
