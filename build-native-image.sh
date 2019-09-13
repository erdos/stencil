#!/usr/bin/env bash

# Compiles a standalone stencil application to a native executable using GraalVM.

set -e

# cd to script dir
cd "$(dirname "$0")"

# check if native-image command is available
hash native-image 2>/dev/null || { echo >&2 "Missing native-image command. Aborting."; exit 1; }

# check if strip command is available
hash strip 2>/dev/null || { echo >&2 "Missing strip command. Aborting."; exit 2; }

# check if uberjar has been compiled so far
TARGET_JAR=$(echo target/*-standalone.jar)

if [ ! -f "$TARGET_JAR" ]
then
    echo "Stencil standalone is not compiled. Compile it first!"
    exit 4
fi

OUTPUT=stencil-native

# build it
native-image \
    -jar $TARGET_JAR \
    -H:ReflectionConfigurationFiles=./reflectconfig \
    -H:-MultiThreaded \
    -H:IncludeResources='.*.txt$' \
    --report-unsupported-elements-at-runtime \
    --initialize-at-build-time \
    $OUTPUT

# remove unused symbols and sections
strip $OUTPUT

echo "Generated $OUTPUT file!"
