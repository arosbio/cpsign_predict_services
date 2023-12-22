# CPSign REST Services

Web services for running [CPSign](https://arosbio.com/) predictive models as REST services. The models should be generated using the open source program [CPSign](https://github.com/arosbio/cpsign) - see preprint [CPSign at bioRxiv](https://www.biorxiv.org/content/10.1101/2023.11.21.568108v1). 

## Table of Contents <!-- omit in toc -->
- [Introduction](#introduction)
    - [Further reading](#further-reading)
- [Quick start](#quick-start)
- [License](#license)
- [Custom build](#custom-build)
- [Developer info](#developer-info)


## Introduction

Currently this server supports models trained from version `2.0.0` of CPSign. These REST services are automatically documented using [OpenAPI](https://www.openapis.org/) by Swagger annotations of the code. Optionally the services can bundle in a [drawing interface](#draw-gui) where molecules can be drawn in a [JSME](https://jsme-editor.github.io/) editor and generate images of atom contributions to the predictions as well as the [Swagger UI](#swagger-ui) for viewing/rendering the OpenAPI definition in a more human readable format.

### Further reading
- CPSign is now available as a preprint at bioRxiv: [CPSign - Conformal Prediction for Cheminformatics Modeling](https://www.biorxiv.org/content/10.1101/2023.11.21.568108v1). 
- CPSign has a [Readthedocs](https://cpsign.readthedocs.io/) web page with user documentation.

## Quick start
We now publish base docker images using GitHub packages which simplifies spinning up your own services. See the available images and tags at the [Packages tab](https://github.com/orgs/arosbio/packages?repo_name=cpsign_predict_services) at GitHub. There are one type of service for each type of CPSign model;
- **cpsign-cp-clf-server**: Conformal classification models, supports both ACP and TCP models.
- **cpsign-cp-reg-server**: Conformal regression models.
- **cpsign-vap-clf-server**: Venn-ABERS probabilistic models. 

To build a Docker image that includes your model you only need these lines in a Dockerfile (excluding the comment lines):
```Docker
# Pick the base image to use
FROM ghcr.io/arosbio/[IMAGE-NAME]:[TAG]
# Copy your model to the default model-path
COPY [your-model-file] /var/lib/jetty/model.jar
```
This assumes that you have the model in the same directory as your Dockerfile. Building your image is then easily performed by:
```bash
docker build -t [YOUR TAG] .
```
And then you can start it up by:
```bash
docker run -p 80:8080 -u jetty [YOUR TAG]
```
The service exposes port 8080 in the container and the `docker run` command maps it to port 80 on your local machine - which allows you to check that it works using your standard web browser;
```
http://localhost/api/v2/modelInfo
# or:
http://localhost/api/v2/health
```

**Note:** The base Docker images do not include the [draw UI](#draw-gui) or [Swagger UI](#swagger-ui), they are intended to be as lightweight as possible. If the drawing UI is required to be included in each service, we refer to the [Custom build section](#custom-build)

## License
The CPSign program is dual licensed, see more in the [CPSign base repo](https://github.com/arosbio/cpsign#license). This extension is published under the [GNU General Public License version 3 (GPLv3)](http://www.gnu.org/licenses/gpl-3.0.html).


## Custom build
When custom you have custom requirements, e.g. wish to include the [draw UI](#draw-gui), here comes more details that required to build the services yourself. 


### Repo layout
This repo contains a Maven Parent pom in the root and three service-implementations (`cp_classification`, `cp_regression` and `vap_classification`) which handles the three types of CPSign predictive models that can be deployed. To reduce the code duplication a separate java project (`service_utils`) is used for grouping common utility function and models, so that updates can be applied more easily and pushed to all concrete model services. The folder `web_res` in the root is used for common resources such as the [Swagger UI](https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/) and some common code for the [draw GUI](#draw-gui). These resources are optionally included in the final WAR application (see more below). 


### Building and deployment
Building the WAR files are done either from one of the child modules (building a single service WAR) or from the root (parent) module (to build all three WARs). There are two profiles (`full` and `thin`), where the `full` profile is active by default. The `full` profile bundles in static files for serving the [Swagger UI](#swagger-ui) and [draw GUI](#draw-gui) as part of the WAR application and thus make each service slightly larger. The `thin` profile excludes these static components and thus create more lightweight services (difference is around 7MB). 
```
# Build the full:
mvn package -DskipTests
# Build the using the thin profile
mvn package -DskipTests -P thin
```

The generated WAR files are saved in each service `/target` folder using the default `<service-name>-<version>.war` name (but the WAR name can be modified by adding the optional argument `-DfinalName=<your name>`). This can simply be dropped in and deployed in a Jetty server together with the model you wish to deploy, currently tested and built for Jetty 11.0.18.

#### Start up
When starting a prediction service the server will need the prediction model that should be used, this can be injected and specified in three different ways and handled (in order):

1. Firstly, the code will check if the environment variable `MODEL_FILE` is set, if it is set it will handle this as a URI to a model. If the content of `MODEL_FILE` either is not pointing to a model, or if the model is non-compatible in any way, the setup will fail.
2. Secondly, the code with check if a JVM property `MODEL_FILE` was set and try to load this as a URI pointing to a model. Setup will fail in case the URI is invalid or points to an invalid/incompatible file/model.
3. As a final step, the server will check in the location `/var/lib/jetty/model.jar`. If there is no model the setup will fail.

In case the setup fails, the web server will still be running but all calls to the REST endpoints should return a HTTP 503 error code.

### Docker build
If you wish to deploy your services using Docker you can look at our multi-stage [Dockerfile](Dockerfile) which builds all three server types. If you simply wish to include the drawing GUI you only need to remove the `-P thin` in the maven build line in the `war-builder` step.
Note that the base jetty image can be replaced with the `alpine` version in order to make the container slimmer, but as it is not supported by all platforms we have opted for the default jetty image for version 11.0.18 for our base docker images - but you might be able to slim down your services by testing out the alpine version. 

## Swagger UI
The prediction services are documented by an OpenAPI definition, which is located at `<server-url>:<optional-port>/api/openapi.json` once the REST service is up and running. This is however rater hard for a human to read, why we recommend to use the [Swagger UI](https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/) for an easier way to view it. Users can either download and run the Swagger UI locally or from another web service by pointing to your server-URL, or for convenience we make it possible to add the Swagger UI static files to the WAR files themselves in which case the Swagger UI is accessible within the service from the root URL of the service, e.g. `http://localhost:8080` in case you run it locally using e.g. the `start_test_server.sh` script and use the default settings. 

## Draw GUI
Each service can optionally include a GUI where molecules can be drawn in one window and the atom gradient can be viewed interactively, i.e. see what parts of the molecule had the greatest impact on the prediction. If the WAR file is built with the profile `full` this interface is accessible from the URL `<service-url>:<optional-port>/draw` and looks e.g. in this way;

![Draw GUI](/readme_imgs/draw_ui.png)

In case you run the `start_test_server.sh` this web page is accessible from `http://localhost:8080/draw/`. 

## Check service health
The services all has a REST endpoint at `<service-URL>:<optional-port>/api/v2/health` that returns HTTP 200 if everything is OK or 503 if something is wrong.

## Developer info
This section is intended for developers. 


### Software testing

#### Requirements
For all tests to run the services each need a valid model. For each service the tests rely on having a valid model (for the given service type) at location `src/test/resources/test-model.cpsign`. For the `cp_classification` service there's also an alternative test-suite for TCP which requires a TCP-model in the location `src/test/resources/test-model-tcp.cpsign` 

#### Unit-testing utility code
There are a few unit-tests, i.e. tests that do not rely on having a web server running. These are executed by running `mvn test` from the root directory. 

#### Integration tests 
The integration tests requires a running REST service. These tests are run using `mvn verify` but in order for this to work, the environment variable `MODEL_FILE` must be set (and pointing to the correct model for each service type). Thus, these tests **do not work** by running `mvn verify` from the maven parent - instead each service type must be tested one-by-one. To make this simpler there's a `run_IT_tests.sh` script within each service module that sets these variables and runs the integration tests (i.e. pointing to the `src/test/resources/test-model.cpsign` model within each child module, then runs `mvn verify`). There is also a 'master script' in the root directory that calls each service test-script so all integration tests can be run from a single script.

#### User-interactive testing
To facilitate easy interactive testing, each of the services includes a shell script (`start_test_server.sh`) that sets the environment variable to point to the test-model (see [Requirements](#requirements)) and spins up a jetty server for the user to try it out.





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
* Using Jackson annotation such as `@JsonProperty("code")` on the models, which Swagger-core picks up and puts in the OpenAPI definition and for Jackson conversion POJO to json/xml

