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

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;

import org.json.simple.JSONObject;

/**
 * BadRequestError
 */
@ApiModel(description = "BadRequestError")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-10-06T12:54:15.929Z")
public class BadRequestError   {
	@JsonProperty("code")
	private Integer code = null;

	@JsonProperty("message")
	private String message = null;

	@JsonProperty("fields")
	private List<String> fields = new ArrayList<String>();

	public BadRequestError(int code, String message, List<String> fields) {
		this.code = code;
		this.message = message;
		this.fields = fields;
	}

	public BadRequestError code(Integer code) {
		this.code = code;
		return this;
	}

	/**
	 * HTTP status code
	 * @return code
	 **/
	@JsonProperty("code")
	@ApiModelProperty(required = true, value = "HTTP status code")
	@NotNull
	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public BadRequestError message(String message) {
		this.message = message;
		return this;
	}

	/**
	 * Error message
	 * @return message
	 **/
	@JsonProperty("message")
	@ApiModelProperty(required = true, value = "Error message")
	@NotNull
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public BadRequestError fields(List<String> fields) {
		this.fields = fields;
		return this;
	}

	public BadRequestError addFieldsItem(String fieldsItem) {
		this.fields.add(fieldsItem);
		return this;
	}

	/**
	 * Relevant field(s)
	 * @return fields
	 **/
	@JsonProperty("fields")
	@ApiModelProperty(required = true, value = "Relevant field(s)")
	@NotNull
	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> fields) {
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
		return Objects.equals(this.code, badRequestError.code) &&
				Objects.equals(this.message, badRequestError.message) &&
				Objects.equals(this.fields, badRequestError.fields);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, message, fields);
	}


	@SuppressWarnings("unchecked")
	@Override
	public String toString() {
		JSONObject jsonResponse = new JSONObject();

		jsonResponse.put("code", getCode());
		jsonResponse.put("message", getMessage());
		jsonResponse.put("fields", fields);

		return jsonResponse.toJSONString();
	}
}