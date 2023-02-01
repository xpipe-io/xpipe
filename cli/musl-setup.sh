#!/bin/sh
# This script builds the bundle inside the Docker container and then copies it to the host machine.

set -e
# Set up the URL to the version of musl used by alpine at the time of writing.
LATEST_MUSL_URL="http://more.musl.cc/10/x86_64-linux-musl/x86_64-linux-musl-native.tgz"
# Set up the URL for the latest zlib version available at the time of writing.
LATEST_ZLIB_URL="https://www.zlib.net/zlib-1.2.13.tar.gz"
BUNDLE_DIR_NAME="bundle"
DIR="$1"
BUILD_DIR="$DIR/build/musl"

# Create the folder that will contain the finished bundle.
mkdir -p $BUILD_DIR
cd $BUILD_DIR

echo "Downloading musl library."
wget "${LATEST_MUSL_URL}"
echo "Downloading zlib library."
wget "${LATEST_ZLIB_URL}"
# Grab the names of the archives from the URL.
MUSL_TAR="${LATEST_MUSL_URL##*/}"
ZLIB_TAR="${LATEST_ZLIB_URL##*/}"

# Compile musl, compiling only the static musl libraries.
echo "Extracting musl."
tar xvzf "$MUSL_TAR"
MUSL_DIR=$(tar tzf "${MUSL_TAR}" | cut -d'/' -f1 | uniq)
TOOLCHAIN_DIR="${BUILD_DIR=}/${MUSL_DIR}"
echo $TOOLCHAIN_DIR

# Compile zlib with the musl library we just built.
export CC="${TOOLCHAIN_DIR}/bin/gcc"

echo "Extracting zlib."
tar xvzf "${ZLIB_TAR}"
ZLIB_DIR=$(tar tzf "${ZLIB_TAR}" | cut -d'/' -f1 | uniq)
cd "${ZLIB_DIR}"
echo "Configuring zlib."
echo $TOOLCHAIN_DIR
./configure --static --prefix="${TOOLCHAIN_DIR}"
echo "Building zlib."
make
make install