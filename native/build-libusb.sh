#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
test ! -e libusb || rm -rf libusb
git clone --depth 1 --branch v1.0.28 https://github.com/libusb/libusb.git libusb
git -C libusb rev-parse HEAD
git -C libusb archive --format=tar.gz --prefix=libusb-1.0.28/ HEAD > libusb-source.tar.gz
mkdir -p ../app/src/main/jniLibs ../app/src/main/assets/licenses
cp libusb/COPYING ../app/src/main/assets/licenses/libusb-LGPL-2.1.txt
"$ANDROID_HOME/ndk/28.1.13356709/ndk-build" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT="$PWD/Android.mk" APP_ABI="arm64-v8a armeabi-v7a x86 x86_64" APP_PLATFORM=android-28 NDK_LIBS_OUT="$PWD/../app/src/main/jniLibs" NDK_OUT="$PWD/obj" -j2
