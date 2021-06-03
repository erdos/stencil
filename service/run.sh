#!/usr/bin/env sh

# starts a stencil service docker container.
# - first parameter: directory of template files
# - second parameter: http port to listen on.
#
# Use the STENCIL_JAVA_OPTIONS environment variable to adjust the JVM in the container.
#
# for example to limit RAM usage: STENCIL_JAVA_OPTIONS="-XX:+PrintFlagsFinal -XX:MaxRAM=500m" ./run.sh
#

STENCIL_HOST_TEMPLATE_DIR=${1:-/home/erdos/Work/stencil/test-resources}
STENCIL_HOST_HTTP_PORT=${2:-8080}
STENCIL_VERSION=${3:-"ghcr.io/erdos/stencil:latest"}
export STENCIL_JAVA_OPTIONS=${STENCIL_JAVA_OPTIONS:-"-XX:+PrintFlagsFinal"}

if command -v docker &> /dev/null
then
    COMMAND=docker
elif command -v podman &> /dev/null
then
    COMMAND=podman
else
    echo "Neither Docker nor Podman was found."
    exit 3
fi

$COMMAND run -it \
         -p $STENCIL_HOST_HTTP_PORT:8080 \
         -v $STENCIL_HOST_TEMPLATE_DIR:/templates \
         --security-opt label=disable \
         -e STENCIL_JAVA_OPTIONS \
         $STENCIL_VERSION
