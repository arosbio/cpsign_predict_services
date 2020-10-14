package com.arosbio.api.model;

import java.util.Objects;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorResponse {
	@JsonProperty("code")
	private final int code;

	@JsonProperty("message")
	private final String message;

	public ErrorResponse(Status status, String message) {
		this.code = status.getStatusCode();
		this.message = message;
	}
	
	public ErrorResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}
	
	public int getCode() {
		return code;
	}
	public String getMsg() {
		return message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ErrorResponse error = (ErrorResponse) o;
		return Objects.equals(this.code, error.code) &&
				Objects.equals(this.message, error.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, message);
	}

	@Override
	public String toString() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "Error Reponse code=" + code + ", msg=" + message;
		}
	}

}

