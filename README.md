# README

Prediction service for running cpsign predictive models as REST services.

## This repo
This repo contains a Maven Parent pom in the root and three service-implementations (`cp_classification`, `cp_regression` and `vap_classification`). To reduce the code duplication a separate java project (`service_utils`) is used for grouping common utility function and models, so that updates can be applied more easily and pushed to all underlying model services. The folder `web_res` in the root is used for common things that should be put in the final WAR folder, which is handled using the [maven WAR plugin <webResources> tag](https://maven.apache.org/plugins/maven-war-plugin/examples/adding-filtering-webresources.html). 

### `web_res`
Currently we bundle the [Swagger UI](https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/) version 3.52.2 using 'standalone installation' and the JSME app. If updates are needed, now there's a single location to make them. 

## Testing

### Requirements
For all tests to run the services needs a valid model. For each service the tests rely on having a valid model (for the given service type) at location `src/test/resources/test-model.cpsign`. For the `cp_classification` service there's also an alternative test-suite for TCP which requires a TCP-model in the location `src/test/resources/test-model-tcp.cpsign` 

### Unit-testing utility code
There is test suite in [service_utils](service_utils/src/test/java/suites/UnitTestSuite.java) that runs tests that are serverless and quick to run.

### Integration tests 
The integration tests requires a running service, meaning that they need a model that is loaded on startup so we have a fully functional service. These tests are run using `mvn verify` but in order for this to work, the system-property `MODEL_FILE` must be set. To make this simpler there's a `run_IT_tests.sh` within each repo that sets these variables and runs the integration tests. 

### User-interactive testing
To facilitate easy interactive testing, each of the services includes a shell script that sets the environment variables to point to the resources outlined under Requirements and starts up the service.  

### DIY - testing 
As you can read in the following sections, the services require a model to work. Testing can then be performed using the maven-jetty-plugin, in the terminal you can thus run:
```
export MODEL_FILE=<path-to-model>
mvn clean package jetty:run-war -DskipTests=true
```

Currently there are a lot of warnings produced due to duplicate classes encountered when scanning the classpath when starting up the server (due to CPSign bundling all dependencies, so maven cannot resolve conflicts). But once finished you should be able to access the Swagger UI at:
`
http://localhost:8080
`
The raw api-definition at:
`
http://localhost:8080/api/openapi.json
` 

The drawing UI at:
`
http://localhost:8080/draw/
`

## Deployment
Building the services is done using maven, using the parent pom you can simply run the following command from the root of the project which will build a WAR file in each child-projects `/target` directory:
`
mvn clean package -DskipTests=true
`

### Runtime
The services has been tested and developed with Jetty 9.4.32.v20200930 so it will likely be easiest to get things up and running with this server.

### Start up
When starting a prediction service the server will need a model, this can be injected and specified in two different ways:

First the server will check if the environment variable `MODEL_FILE` is set, if it is set it will handle this as URI to a model. Otherwise the server will check in the location `/var/lib/jetty/model.jar`. If there is no model the setup will fail.

### Check service health
The services all has a REST endpoint at `<service-URL>/api/v2/health` that returns HTTP 200 if everything is OK or 503 if something is wrong.

## TODOs:
- [ ] Add Dockerfile for how to start a server 
- [ ] update draw GUI to use [ketcher](https://lifescience.opensource.epam.com/ketcher/index.html) drawer
- [ ] add config to startup to include/exclude the draw GUI and swagger UI files - to make it possible to have as small services as possible
- [ ] look over updates on jetty, swagger etc
- [ ] refactor the "draw" thing as a separate folder that is pulled in during maven build
- [x] CORSFilter update?
- [x] Remove jackson included both in cpsign and from swagger stuff
- [x] Set up logging properly - need a logging config file 
- [x] add Prediction-starter servlet thingy to web.xml again
- [x] local repo for CPSign in the root of git-repo, not for each individual Eclipse-project


## Implementation details
* Using JAX-RS annotations for the REST api
* Using Jersey runtime "environment" and tooling
* Using Jetty server (due to being light weight)
* Using Jackson annotation such as @JsonProperty("code") on the models, which Swagger-core picks up and puts in the OpenAPI definition and for Jackson conversion POJO java to json/xml

