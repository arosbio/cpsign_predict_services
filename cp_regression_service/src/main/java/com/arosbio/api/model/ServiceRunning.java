package com.arosbio.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceRunning {
	
	@JsonProperty
	private final String message="Service running";
	
	public String toString() {
		return message;
	}
	
}
