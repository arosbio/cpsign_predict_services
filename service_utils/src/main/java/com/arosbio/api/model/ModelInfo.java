package com.arosbio.api.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelInfo {
	
	@JsonProperty()
	@NotNull
	public String modelName;
	
	@JsonProperty()
	@NotNull
	public String version;
	
	@JsonProperty()
	public String category;
	
	public ModelInfo(com.arosbio.ml.io.ModelInfo info) {
		this.modelName = info.getName();
		this.version = info.getVersion().toString();
		this.category = info.getCategory();
		if (category != null && category.isEmpty())
			category = null; // set to null to remove from json output
	}

}
