#!/usr/bin/env bash
# Installs the Android SDK pieces this project needs and points Gradle at them.
# Runs automatically when the Codespace is created; safe to run again by hand:
#   bash .devcontainer/setup-android.sh
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v unzip >/dev/null 2>&1; then
  echo "Installing unzip..."
  sudo apt-get update -qq && sudo apt-get install -y -qq unzip
fi

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Downloading Android command-line tools..."
  mkdir -p "$SDK/cmdline-tools"
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

export ANDROID_HOME="$SDK"
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"

echo "Accepting SDK licences..."
yes | sdkmanager --licenses >/dev/null 2>&1 || true

echo "Installing platform 36 and build tools..."
sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;35.0.0" >/dev/null

echo "sdk.dir=$SDK" > "$ROOT/local.properties"
chmod +x "$ROOT/gradlew"

echo
echo "Android SDK ready at $SDK"
echo "Build the app with:   ./gradlew assembleDebug"
echo "The APK is written to: app/build/outputs/apk/debug/app-debug.apk"
