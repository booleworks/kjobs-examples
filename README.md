# KJobs Examples

This is a collection of examples for the [KJobs Framework](https://github.com/booleworks/kjobs). *It is currently WIP, more examples will follow in the future.*

## Running the Examples

Each example has its own package below `com.booleworks.kjobs.examples` which contains a `main` method to start a new web service with this example. In the `tests` folder, you will find example HTTP requests for each example.

### Starting the Application

Simply start the web service using the `main` function of the example (e.g. in `BasicUsage.kt`). By default, the web service will be deployed on `localhost:8080` which you can change globally in the `ExampleConfig` object.

If you want to start the service from the command line, use `mvn exec:java -Dexec.mainClass="com.booleworks.kjobs.examples.basicusage.BasicUsageKt"`.

### Running HTTP Requests

The `tests` folder contains a couple of HTTP requests for each example. The files are designed to be used with the IntelliJ HTTP Client, but they can easily be transformed to be used in other tools like `httpie`.

The examples are usually intended to be run from top to bottom. In some cases you have to wait and call the resource repeatedly to wait for some condition (e.g. the job status to return `SUCCESS`).

If you have launched the service on another host or port, adjust the `host` variable in `tests/http-client.env.json`.

## TODO

Further examples:
- Multiple APIs
- Advanced configuration
- Redis
- Hierarchical Jobs
