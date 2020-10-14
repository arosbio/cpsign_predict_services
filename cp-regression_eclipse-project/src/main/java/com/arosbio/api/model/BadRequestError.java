package com.arosbio.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BadRequestError extends ErrorResponse {

	@JsonProperty("fields")
	private List<String> fields = new ArrayList<String>();

	public BadRequestError(int code, String message, List<String> fields) {
		super(code, message);
		this.fields = fields;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BadRequestError badRequestError = (BadRequestError) o;
		return super.equals(badRequestError) &&
				Objects.equals(this.fields, badRequestError.fields);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fields);
	}


	@Override
	public String toString() {
		return super.toString();
	}
}