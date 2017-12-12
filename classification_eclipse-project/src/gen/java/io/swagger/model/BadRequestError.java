/*
 * Predict with a CPSign classification model
 * Service that deploys a CPSign classification model and allows for predictions to be made by the deployed model.
 *
 * OpenAPI spec version: 0.1.0
 * Contact: info@genettasoft.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * BadRequestError
 */
@ApiModel(description = "BadRequestError")
public class BadRequestError extends Error {

	@JsonProperty("fields")
	@ApiModelProperty(required = true, value = "Relevant field(s)", allowableValues="smiles")
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


	@SuppressWarnings("unchecked")
	@Override
	public String toString() {
		JSONObject jsonResponse = super.toJSON();
		jsonResponse.put("fields", fields);
		return jsonResponse.toJSONString();
	}
}