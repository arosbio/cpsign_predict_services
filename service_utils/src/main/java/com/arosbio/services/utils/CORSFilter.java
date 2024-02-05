package com.arosbio.services.utils;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {
	
	public static final String ALLOWED_ORIGIN_ENV_VARIABLE = "ALLOWED_ORIGIN";
	private static final String ALLOWED_ORIGIN;

	static {
		// Try environment variable first
		String allowedHosts = System.getenv(ALLOWED_ORIGIN_ENV_VARIABLE);

		// If not set - try system property
		if (allowedHosts == null || allowedHosts.isEmpty()){
			allowedHosts = System.getProperty(ALLOWED_ORIGIN_ENV_VARIABLE);
		}
		// Fallback to allow all origins 
		if (allowedHosts == null || allowedHosts.isEmpty()){
			allowedHosts = "*";
		}
		ALLOWED_ORIGIN = allowedHosts;
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		responseContext.getHeaders().add("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
		responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
		responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
		responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD");
	}
}