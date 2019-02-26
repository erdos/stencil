# Stencil Fn Project container

This is an example application that creates a [Fn Project](https://fnproject.io) container to run Stencil as a Serverless application.

Install Docker (tested with 18.09.2, API 1.39) and the Fn CLI (client 0.5.51, server 0.3.663) before starting.


## Starting

1. Type `fn version` to check the Fn client and server versions
2. Start a Fn Project server: `fn start`
3. Type `fn list contexts` to see the installed applications
4. Deploy an endpoint from the current directory: `fn deploy --all --local --no-bump`


 Call deployed application with the following CURL command:

    curl http://localhost:8080/t/helloapp/yoyo --data "{}"

