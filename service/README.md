# Dockerized service for Stencil templates

This is a Dockerized example application running the Stencil template engine.
You can use


## Usage

Building and running the container:

1. Build the container with the `build.sh` command.
2. Run the container locally with the `run.sh` command. First parameter: directory of template files to mount as a volume. Second parameter: http port to listen on (defaults to `8080`).

After you start the container, it will enumerate and print the names of
the template files it found in the volume.

Rendering a template:

```
time curl -XPOST localhost:8080/test-control-loop.docx --header "Content-Type: application/json" --data '{"elems":[{"value": "first"}]}' > rendered.docx
```

Opening the output file shows that the file contents are rendered all right.

```
oowriter rendered.docx
```


## API

You can send requests over a HTTP api.

**request:**

- method: `POST`
- uri: relative path of template file in templates folder
- headers: `Content-Type: application/json`
- request body: a JSON map of template data.

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
