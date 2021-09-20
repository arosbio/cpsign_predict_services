package com.arosbio.api.model;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClassificationResult {
	
	@JsonProperty("smiles")
	@NotNull
	public String smiles;
	
	@JsonProperty("prediction")
	public List<PValueMapping> prediction;
	
	@JsonProperty("modelName")
	@NotNull
	public String modelName;
	
	public ClassificationResult(List<PValueMapping> pvalues, String smiles, String modelName) {
		this.smiles = smiles;
		this.prediction = pvalues;
		this.modelName = modelName;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClassificationResult classification = (ClassificationResult) o;
		return Objects.equals(this.smiles, classification.smiles) &&
				Objects.equals(this.prediction, classification.prediction) &&
				Objects.equals(this.modelName, classification.modelName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(smiles, prediction, modelName);
	}
	
}

