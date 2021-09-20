package com.arosbio.api.model;

import javax.validation.constraints.NotNull;

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
	
	public ModelInfo(com.arosbio.modeling.io.ModelInfo info) {
		this.modelName = info.getModelName();
		this.version = info.getModelVersion().toString();
		this.category = info.getModelCategory();
		if (category != null && category.isEmpty())
			category = null; // set to null to remove from json output
	}

}
