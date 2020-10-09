FROM clojure:openjdk-8-lein-2.9.3-slim-buster AS build-env
WORKDIR /usr/src/myapp

COPY project.clj /usr/src/myapp/
RUN lein deps

COPY . /usr/src/myapp

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" myapp-standalone.jar

FROM openjdk:8-jre-slim-buster

ENV STENCIL_HTTP_PORT 8080
ENV STENCIL_TEMPLATE_DIR /templates
ENV STENCIL_JAVA_OPTIONS -XX:+PrintFlagsFinal

VOLUME /templates

WORKDIR /myapp
COPY --from=build-env /usr/src/myapp/myapp-standalone.jar /myapp/myapp.jar
ENTRYPOINT java $STENCIL_JAVA_OPTIONS -jar /myapp/myapp.jar
EXPOSE 8080
