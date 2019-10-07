package io.swagger.api.factories;

import io.swagger.api.PredictApiService;
import io.swagger.api.impl.PredictApiServiceImpl;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-10-06T12:54:15.929Z")
public class PredictApiServiceFactory {
    private final static PredictApiService service = new PredictApiServiceImpl();

    public static PredictApiService getPredictApi() {
        return service;
    }
}
