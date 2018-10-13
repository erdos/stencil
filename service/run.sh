#!/usr/bin/env sh

# starts a stencil service docker container.
# - first parameter: directory of template files
# - second parameter: http port to listen on.
#
# Use the STENCIL_JAVA_OPTIONS environment variable to adjust the JVM in the container.
#
# for example to limit RAM usage: STENCIL_JAVA_OPTIONS="-XX:+PrintFlagsFinal -XX:MaxRAM=500m" ./run.sh
#

STENCIL_HOST_TEMPLATE_DIR=${1:-/home/erdos/Joy/stencil/test-resources}
STENCIL_HOST_HTTP_PORT=${2:-8080}
export STENCIL_JAVA_OPTIONS=${STENCIL_JAVA_OPTIONS:-"-XX:+PrintFlagsFinal"}

docker run -it -p $STENCIL_HOST_HTTP_PORT:8080 -v $STENCIL_HOST_TEMPLATE_DIR:/templates -e STENCIL_JAVA_OPTIONS stencil-service:latest
