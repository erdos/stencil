FROM clojure AS clj

# prefetch dependencies so we will not have to download them every single time.
COPY project.clj .
RUN lein deps

COPY ./project.clj /app/project.clj
COPY ./src /app/src

WORKDIR /app

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

FROM oracle/graalvm-ce:1.0.0-rc12 AS BASE

COPY --from=CLJ /app/app-standalone.jar /app/app-standalone.jar
WORKDIR /app

RUN native-image -jar app-standalone.jar


# e44b69907603-H:+ReportUnsupportedElementsAtRuntime

FROM scratch

COPY --from=BASE /lib64/libc.so.6 /lib64/libc.so.6
COPY --from=BASE /lib64/libdl.so.2 /lib64/libdl.so.2
COPY --from=BASE /lib64/libpthread.so.0 /lib64/libpthread.so.0
COPY --from=BASE /lib64/libz.so.1 /lib64/libz.so.1
COPY --from=BASE /lib64/librt.so.1 /lib64/librt.so.1
COPY --from=BASE /lib64/libcrypt.so.1 /lib64/libcrypt.so.1
COPY --from=BASE /lib64/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2
COPY --from=BASE /lib64/libfreebl3.so /lib64/libfreebl3.so

COPY --from=BASE /app/app-standalone /

CMD ["/app-standalone"]
