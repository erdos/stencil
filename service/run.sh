#!/usr/bin/env sh

# starts a stencil service docker container.
# - first parameter: directory of template files
# - second parameter: http port to listen on.

STENCIL_HOST_TEMPLATE_DIR=${1:-/home/erdos/Joy/stencil/test-resources}
STENCIL_HOST_HTTP_PORT=${2:-8080}

docker run -it -p $STENCIL_HOST_HTTP_PORT:8080 -v $STENCIL_HOST_TEMPLATE_DIR:/templates stencil-service:latest
