#!/usr/bin/env bash

# Compiles a standalone stencil application to a native executable.

set -e

if [ -z "$NATIVE_IMAGE_BIN" ]
then
    echo "Environment variable NATIVE_IMAGE_BIN is missing. It should point to the GraalVM native-image binary."
    exit 3
fi

TARGET_JAR=$(echo target/*-standalone.jar)

if [ -z "$TARGET_JAR" ]
then
    echo "Stencil standalone is not compiled. Compile first!"
    exit 4
fi

$NATIVE_IMAGE_BIN \
    -jar $TARGET_JAR \
    -H:ReflectionConfigurationFiles=./reflectconfig \
    --report-unsupported-elements-at-runtime
