FROM clojure:openjdk-11-lein-2.9.6-slim-buster AS build-env

WORKDIR /usr/src/myapp

RUN apt-get update -y && apt-get install -y binutils

COPY project.clj /usr/src/myapp/
RUN lein deps

COPY . /usr/src/myapp

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" myapp-standalone.jar
RUN jlink --strip-debug --add-modules "$(jdeps --print-module-deps --ignore-missing-deps myapp-standalone.jar)" --output /java

FROM debian:10.10-slim

ENV STENCIL_HTTP_PORT 8080
ENV STENCIL_TEMPLATE_DIR /templates
ENV STENCIL_JAVA_OPTIONS -XX:+PrintFlagsFinal

VOLUME /templates

WORKDIR /myapp
COPY --from=build-env /java /java
COPY --from=build-env /usr/src/myapp/myapp-standalone.jar /myapp/myapp.jar
ENTRYPOINT "/java/bin/java" $STENCIL_JAVA_OPTIONS -jar /myapp/myapp.jar
EXPOSE 8080
