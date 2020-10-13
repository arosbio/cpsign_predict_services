package com.arosbio.api;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

@ApplicationPath("/api")
public class RESTApplication extends ResourceConfig {
	
	public RESTApplication() {
		property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
		register(PredictApi.class);
		register(OpenApiResource.class);
	}
	
}



//@ApplicationPath("/v3")
//public class RESTApplication extends Application {
//	
//	@Override
//	public Set<Object> getSingletons(){
//		Set<Object> singlestons = new HashSet<>();
//		singlestons.add(new PredictApi());
//		return singlestons;
//	}
//	
//}