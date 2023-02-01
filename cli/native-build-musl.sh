#!/bin/sh

DIR="$1"
MUSL_DIR="$DIR/build/musl/x86_64-linux-musl-native"

export PATH="$PATH:$MUSL_DIR/bin"
"$DIR/../gradlew" :cli:nativeCompile "-Dorg.gradle.jvmargs=-Xmx2048M"
