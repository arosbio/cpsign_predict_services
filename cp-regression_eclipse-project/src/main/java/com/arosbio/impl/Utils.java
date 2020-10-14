package com.arosbio.impl;

import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.Response;

import com.arosbio.api.model.ErrorResponse;

public class Utils {
	
	private static final int MAX_NUM_STACK_TO_LOGG = 10;

	public static String getStackTrace(Throwable e) {
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] stack = e.getStackTrace();
		
		sb.append(e.getClass());
		sb.append(": ");
		sb.append(e.getMessage());
		for (int i=0; i<MAX_NUM_STACK_TO_LOGG && i<stack.length; i++) {
			sb.append('\n');
			sb.append('\t');
			sb.append(stack[i]);
		}
		
		return sb.toString();
	}
	
	public static String decodeURL(String text) throws MalformedURLException {
		if (text ==null || text.isEmpty())
			throw new IllegalArgumentException("Empty data");

		// Charges should be kept as charges, so we replace the input "+" with URL-encoding of a "+" instead
		if (text.contains("+")) {
			text = text.replace("+", "%2B");
		}

		// Clean the molecule-string from URL encoding
		try {
			return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
		} catch (Exception e) {
			throw new MalformedURLException("Could not decode text");
		}
	}
	
	public static double roundTo3digits(double val){
		return Math.round(val*1000.0)/1000.0;
	}

	public static Response getResponse(ErrorResponse error) {
		return Response.status(error.getCode()).entity(error).build();
		
	}
}
