#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
HERE="$PWD"
ROOT="$(dirname "$HERE")"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
git clone --depth 1 --branch n6.1.4 https://github.com/FFmpeg/FFmpeg.git "$TMP/ffmpeg"
git clone --depth 1 --branch 1.5.1 --filter=blob:none --sparse https://github.com/androidx/media.git "$TMP/media"
git -C "$TMP/media" sparse-checkout set libraries/decoder_ffmpeg
git -C "$TMP/ffmpeg" rev-parse HEAD
git -C "$TMP/media" rev-parse HEAD
git -C "$TMP/ffmpeg" archive --format=tar.gz --prefix=FFmpeg-6.1.4/ HEAD > "$HERE/ffmpeg-source.tar.gz"
SRC="$TMP/ffmpeg"
MEDIA="$TMP/media/libraries/decoder_ffmpeg/src/main"
J="$ROOT/app/src/main/java/androidx/media3/decoder/ffmpeg"
mkdir -p "$J" "$ROOT/app/src/main/assets/licenses"
for file in FfmpegAudioDecoder.java FfmpegAudioRenderer.java FfmpegDecoderException.java FfmpegLibrary.java; do
  cp "$MEDIA/java/androidx/media3/decoder/ffmpeg/$file" "$J/$file"
done
cp "$SRC/COPYING.LGPLv2.1" "$ROOT/app/src/main/assets/licenses/ffmpeg-LGPL-2.1.txt"
cp "$SRC/LICENSE.md" "$ROOT/app/src/main/assets/licenses/ffmpeg-license.md"
NDK="$ANDROID_HOME/ndk/28.1.13356709"
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
for abi in arm64-v8a armeabi-v7a x86 x86_64; do
  case "$abi" in
    arm64-v8a) arch=aarch64; triple=aarch64-linux-android;;
    armeabi-v7a) arch=arm; triple=armv7a-linux-androideabi;;
    x86) arch=x86; triple=i686-linux-android;;
    x86_64) arch=x86_64; triple=x86_64-linux-android;;
  esac
  cc="$BIN/${triple}28-clang"
  cxx="$BIN/${triple}28-clang++"
  build="$TMP/build-$abi"; prefix="$TMP/install-$abi"
  mkdir -p "$build" "$prefix"
  cd "$build"
  "$SRC/configure" --prefix="$prefix" --target-os=android --arch="$arch" --enable-cross-compile \
    --cc="$cc" --cxx="$cxx" --ar="$BIN/llvm-ar" --nm="$BIN/llvm-nm" --ranlib="$BIN/llvm-ranlib" --strip="$BIN/llvm-strip" \
    --enable-shared --disable-static --enable-pic --disable-asm --disable-autodetect \
    --disable-programs --disable-doc --disable-network --disable-everything \
    --disable-avdevice --disable-avformat --disable-avfilter --disable-swscale --disable-postproc \
    --enable-avcodec --enable-avutil --enable-swresample \
    --enable-decoder=aac,ac3,eac3,flac,mp3,opus,vorbis,alac \
    --extra-ldflags='-Wl,-z,max-page-size=16384'
  make -j"$(nproc)"
  make install
  OUT="$ROOT/app/src/main/jniLibs/$abi"
  mkdir -p "$OUT"
  for lib in avcodec avutil swresample; do
    test -f "$prefix/lib/lib$lib.so"
    cp -L "$prefix/lib/lib$lib.so" "$OUT/lib$lib.so"
  done
  "$cxx" -std=c++11 -fPIC -shared -static-libstdc++ -I"$prefix/include" "$MEDIA/jni/ffmpeg_jni.cc" \
    -L"$prefix/lib" -Wl,--no-undefined -Wl,-z,max-page-size=16384 \
    -lswresample -lavcodec -lavutil -llog -landroid -o "$OUT/libffmpegJNI.so"
  "$BIN/llvm-readelf" -d "$OUT/libffmpegJNI.so"
done
