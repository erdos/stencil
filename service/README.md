# Dockerized service for Stencil templates

This is a dockerized application running the stencil template engine.

## Usage

- You have to set up a volume pointing to `/templates` where the template files can be found. Then you can render the templates
- Set `STENCIL_HTTP_PORT` to set the http port this container is listening on. Defaults to `8080`

## API

Make a HTTP request to render a template.

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

- status: `404`
- content: plain text

**response (template error)**

- status: `500`
- content: plain text describing error

**response (eval error)**

- status: `400`
- content: plain text describing error

**response (wrong url)**

This happens when you do not send a `POST` request.

- status `405`
