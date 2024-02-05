package com.arosbio.api;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import com.arosbio.services.utils.CORSFilter;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/api")
public class RESTApplication extends ResourceConfig {
	
	public RESTApplication() {
		property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
		property(ServerProperties.WADL_FEATURE_DISABLE, true);
		register(PredictApi.class);
		register(OpenApiResource.class);
		register(JacksonFeature.class); // Jackson-serialization
		register(CORSFilter.class);
	}
	
}
