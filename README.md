# CPSign REST Services

Web services for running [CPSign](https://arosbio.com/) predictive models as REST services. The models should be generated using the open source program [CPSign](https://github.com/arosbio/cpsign). Currently this server supports models trained from version `2.0.0` of CPSign. These REST services are documented using [OpenAPI](https://www.openapis.org/) by Swagger annotations of the code. Optionally the services can bundle in a [drawing interface](#draw-gui) where molecules can be drawn in a [JSME](https://jsme-editor.github.io/) editor and generate images of atom contributions to the predictions as well as the [Swagger UI](#swagger-ui) for viewing/rendering the OpenAPI definition in a more human readable format. These are then put into the final WAR file of the application, which can then be served using a compatible Java web container.

## Repo layout
This repo contains a Maven Parent pom in the root and three service-implementations (`cp_classification`, `cp_regression` and `vap_classification`) which handles the three types of CPSign predictive models that can be deployed. To reduce the code duplication a separate java project (`service_utils`) is used for grouping common utility function and models, so that updates can be applied more easily and pushed to all concrete model services. The folder `web_res` in the root is used for common resources such as the [Swagger UI](https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/) and some common code for the `draw` GUI. These resources are optionally included in the final WAR application (see more below). 


## Building and deployment
Building the WAR files are done either from one of the child modules (building a single service WAR) or from the root (parent) module (to build all three WARs). There are two profiles (`full` and `thin`), where the `full` profile is active by default. The `full` profile bundles in static files for serving the [Swagger UI](#swagger-ui) and [draw GUI](#draw-gui) as part of the WAR application and thus make each service slightly larger. The `thin` profile excludes these static components and thus create more lightweight services (difference is around 7MB). 
```
# Build the full:
mvn package -DskipTests
# Build the using the thin profile
mvn package -DskipTests -P thin
```

The generated WAR files are saved in each service `/target` folder using the default `<service-name>-<version>.war` name. This can simply be dropped in and deployed in a Jetty server, currently tested and built for Jetty 11.0.14.

### Docker based deployment
We have supplied an example of how a Docker container can be set up for deploying the server in the [docker_example](/docker_example/) directory. This assumes that there is a valid CPSign regression model in the location `cp_regression/src/test/resources/test-model.cpsign` (same location as the tests expects there to be models), but the scripts can be changed for another setup. Note that the base jetty image can be replaced with the `alpine` version in order to make the container slimmer, but as it is not supported by all platforms we have opted for the default jetty image for version 11.0.14 for this example script.

### Alternative deployment
The services has been tested and developed with Jetty 11.0.14 so it will likely be easiest to get things up and running with this server.

#### Start up
When starting a prediction service the server will need the prediction model that should be used, this can be injected and specified in two different ways and handled (in order):

1. The code will check if the environment variable `MODEL_FILE` is set, if it is set it will handle this as URI to a model. If the content of `MODEL_FILE` either is not pointing to a model, or if the model is non-compatible in any way, the setup will fail.
2. Otherwise the server will check in the location `/var/lib/jetty/model.jar`. If there is no model the setup will fail.

In case the setup fails, the web server will still be running but all calls to the REST endpoints should return HTTP 503 error code.

## Testing

### Requirements
For all tests to run the services each need a valid model. For each service the tests rely on having a valid model (for the given service type) at location `src/test/resources/test-model.cpsign`. For the `cp_classification` service there's also an alternative test-suite for TCP which requires a TCP-model in the location `src/test/resources/test-model-tcp.cpsign` 

### Unit-testing utility code
There are a few unit-tests, i.e. tests that do not rely on having a web server running. These are executed by running `mvn test` from the root directory. 

### Integration tests 
The integration tests requires a running REST service. These tests are run using `mvn verify` but in order for this to work, the environment variable `MODEL_FILE` must be set (and pointing to the correct model for each service type). Thus, these tests **do not work** by running `mvn verify` from the maven parent - instead each service type must be tested one-by-one. To make this simpler there's a `run_IT_tests.sh` script within each service module that sets these variables and runs the integration tests (i.e. pointing to the `src/test/resources/test-model.cpsign` model within each child module, then runs `mvn verify`). 

### User-interactive testing
To facilitate easy interactive testing, each of the services includes a shell script (`start_test_server.sh`) that sets the environment variable to point to the test-model (see [Requirements](#requirements)) and spins up a jetty server for the user to try it out.


## Swagger UI
The prediction services are documented by an OpenAPI definition, which is located at `<server-url>:<optional-port>/api/openapi.json` once the REST service is up and running. This is however rater hard for a human to read, why we recommend to use the [Swagger UI](https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/) for an easier way to view it. Users can either download and run the Swagger UI locally or from another web service by pointing to your server-URL, or for convenience we make it possible to add the Swagger UI static files to the WAR files themselves in which case the Swagger UI is accessible within the service from the root URL of the service, e.g. `http://localhost:8080` in case you run it locally using e.g. the `start_test_server.sh` script and use the default settings. 

## Draw GUI
Each service can optionally include a GUI where molecules can be drawn in one window and the atom gradient can be viewed interactively, i.e. see what parts of the molecule had the greatest impact on the prediction. If the WAR file is built with the profile `full` this interface is accessible from the URL `<service-url>:<optional-port>/draw` and looks e.g. in this way;

![Draw GUI](/readme_imgs/draw_ui.png)

In case you run the `start_test_server.sh` this web page is accessible from `http://localhost:8080/draw/`. 

### Check service health
The services all has a REST endpoint at `<service-URL>:<optional-port>/api/v2/health` that returns HTTP 200 if everything is OK or 503 if something is wrong.

## TODOs:
- [ ] update draw GUI to use [ketcher](https://lifescience.opensource.epam.com/ketcher/index.html) drawer. This is more frosting on the cake, will be postponed for now.
- [x] Add Dockerfile for how to start a server 
- [x] add config to startup to include/exclude the draw GUI and swagger UI files - to make it possible to have as small services as possible
- [x] look over updates on jetty, swagger etc
- [x] refactor the "draw" thing as a separate folder that is pulled in during maven build
- [x] CORSFilter update?
- [x] Remove jackson included both in cpsign and from swagger stuff
- [x] Set up logging properly - need a logging config file 
- [x] add Prediction-starter servlet thingy to web.xml again
- [x] local repo for CPSign in the root of git-repo, not for each individual Eclipse-project


## Implementation details
* Using JAX-RS annotations for the REST api
* Using Jersey runtime "environment" and tooling
* Using Jetty server (due to being light weight)
* Using Jackson annotation such as @JsonProperty("code") on the models, which Swagger-core picks up and puts in the OpenAPI definition and for Jackson conversion POJO to json/xml

