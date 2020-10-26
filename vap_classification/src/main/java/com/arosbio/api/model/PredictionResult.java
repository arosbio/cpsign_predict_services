package com.arosbio.api.model;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionResult   {
	
	@JsonProperty("smiles")
	@NotNull
	private final String smiles;

	@JsonProperty("prediction")
	private List<ProbabilityMapping> prediction;
	
	@JsonProperty("modelName")
	@NotNull
	private final String modelName;

	public PredictionResult(List<ProbabilityMapping> probabilities, String smiles, String modelName) {
		this.smiles = smiles;
		this.prediction = probabilities;
		this.modelName = modelName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PredictionResult venaber = (PredictionResult) o;
		return Objects.equals(this.smiles, venaber.smiles) &&
				Objects.equals(this.prediction, venaber.prediction) &&
				Objects.equals(this.modelName, venaber.modelName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(smiles, prediction, modelName);
	}

}

