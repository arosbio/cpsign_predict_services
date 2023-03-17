package com.arosbio.services.utils;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		String allowedHosts = System.getenv("ALLOWED_HOSTS");
		if( allowedHosts!=null && !allowedHosts.isEmpty()) {
			responseContext.getHeaders().add("Access-Control-Allow-Origin", allowedHosts);
			responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
			responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
			responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
		}
	}
}