#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UP="$ROOT/receiver-combined"
UPSTREAM=7152adf13a1e5a55799da12f841c8016d1f5cfe0
test ! -e "$UP"
git init "$UP"
git -C "$UP" remote add origin https://github.com/andreknieriem/open-headunit.git
git -C "$UP" fetch --depth 1 origin "$UPSTREAM"
git -C "$UP" checkout --detach FETCH_HEAD
test "$(git -C "$UP" rev-parse HEAD)" = "$UPSTREAM"
cp -a "$ROOT/app" "$UP/cockpit"
python3 "$ROOT/native/merge-headunit.py" "$UP"
cd "$UP"
chmod +x gradlew
./gradlew --no-daemon :app:assembleGithubDebug --stacktrace
mkdir -p "$ROOT/combined-output"
mapfile -t apks < <(find app/build/outputs/apk/github/debug -name '*.apk' -type f)
test "${#apks[@]}" -eq 1
cp "${apks[0]}" "$ROOT/combined-output/drivecore-headunit-experimental.apk"
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs "$ROOT/combined-output/drivecore-headunit-experimental.apk"
./gradlew --no-daemon :app:dependencies --configuration githubDebugRuntimeClasspath > "$ROOT/combined-output/runtime-dependencies.txt"
# Corresponding source and instructions, never include signing keys or build caches.
# Upstream's LGPL media binaries are replaced by our own source-built artifacts.
tar -czf "$ROOT/combined-output/combined-source.tar.gz" \
  --exclude=.git --exclude=.gradle --exclude=build --exclude=.idea \
  --exclude='*.jks' --exclude='*.p12' --exclude='*.jkc' --exclude='*.keystore' \
  --exclude='local.properties' --exclude='secrets.properties' --exclude='key.properties' \
  -C "$ROOT" receiver-combined native/merge-headunit.py native/build-headunit.sh native/build-libusb.sh native/build-ffmpeg.sh native/Android.mk native/usb_probe.c native/libusb-source.tar.gz native/ffmpeg-source.tar.gz docs/HEADUNIT-INTEGRATION.md
