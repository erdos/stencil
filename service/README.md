# Dockerized service for Stencil templates

This is a Dockerized example application running the Stencil template engine.
You can use this service to render template documents from programs written in any programming language using only a HTTP API. You can also use this container to horizontally scale the rendering capacity of your application.

## Requirements

Requires Docker 17.05 or higher (for multi-stage builds).

## Usage

Building and running the container:

1. Build the container with the `docker build .` command. You can also use Podman instead of Docker.
2. Run the container locally with the `run.sh` command. First parameter: directory of template files to mount as a volume. Second parameter: http port to listen on (defaults to `8080`). Optional third parameter is the hash of the container image to run.

You can use the `STENCIL_JAVA_OPTIONS` environment variable to specify custom options for the JVM running inside the container.

After you start the container, it will enumerate and print the names of the template files it found in the volume.

Rendering a template:

```
time curl -XPOST localhost:8080/test-control-loop.docx --header "Content-Type: application/json" --data '{"elems":[{"value": "first"}]}' > rendered.docx
```

Opening the output file shows that the file contents are rendered all right: `oowriter rendered.docx`


## API

You can send requests over a HTTP API. At the moment only one request is supported: preparing and rendering a template file.

Preparing the template file takes place at the first request for each template file. The prepared template is cached. Therefore, the first request may take a little longer than the rest.

Changing the template files on the file system will force preparing the template files again.

**Liveness endpoint**

Call `GET /` for a liveness status check.

**Document generation requests:**

- method: `POST`
- uri: relative path of template file in templates folder
- headers: `Content-Type: application/json`
- request body: a JSON map of template data.

You can add the `X-Stencil-Log: debug` or `X-Stencil-Log: trace` header to specify the log level while serving the request.

You can also specify the `X-Stencil-Corr-Id: ...` header to set a [Correlation Id](https://en.wikipedia.org/wiki/Identity_correlation) that will be used in the logs. By default, a random value will be generated on each request.

The different responses have different HTTP staus codes.

**response (success)**

- status: `200`
- headers: `Content-Type: application/octet-stream`
- content: rendered document ready to download.

**response (template not found)**

This happens then the given file name was not found in the template directory.

- status: `404`
- content: plain text

**response (template error)**

This happens when the template file could not be prepared. For example: syntax
errors in template file, unexpected file format, etc.

- status: `500`
- content: plain text describing error

**response (eval error)**

This happens when the template could not be evaluated.

- status: `400`
- content: plain text describing error

**response (wrong url)**

This happens when you do not send a `POST` request.

- status `405`

### Logging

Logs are printed to standard output in the following format:

```
date-time log-level namespace correlation-id : log-message
```

For example:

```
2021-08-15T15:32:44.025129Z INFO stencil.service 71631d49 : Successfully rendered template /test-functions.docx
```

You can bind the `STENCIL_LOG_LEVEL` environment variable to change the default logging level from `info`. Acceptible values: `trace`, `debug`, `info`, `warn`, `error`, `fatal`.

### Custom Functions

It is possible to extend Stencil service with custom functions defined in Javascript. Add a `stencil.js` file in the template directory so it can be read and executed when the service loads.
The functions defined in the script will be available in the templates when rendered.

The [Rhino](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino) scripting engine is used for evaluating the scripts.

In current version, the script file is not reloaded automatically, therefore a service restart is necessary on changes.

Add a `stencil.js` file in the template directory with the following contents:
```
function daysBetweenTimestamps(ts1, ts2) {
    var time1 = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(ts1), java.util.TimeZone.getDefault().toZoneId());  
    var time2 = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(ts2), java.util.TimeZone.getDefault().toZoneId());  
    var duration = java.time.Duration.between(time1, time2);
    return duration.toDays();
}
```

The function now can be called in the templates like the following:
```
   {%=daysBetweenTimestamps(1499070300, 1644956311)%}
```
