package com.arosbio.api.model;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.arosbio.impl.Utils;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class RegressionResult {
	
	@JsonProperty("smiles")
	@NotNull
	private final String smiles;
	
	@JsonProperty("lower")
	private final Double lower;

	@JsonProperty("upper")
	private final Double upper;

	@JsonProperty("predictionMidpoint")
	private final Double predictionMidpoint;

	@JsonProperty("confidence")
	private final Double confidence;
	
	@JsonProperty("modelName")
	@NotNull
	private final String modelName;

	public RegressionResult(String smiles, double lower, double upper, double mp, double confidence, String modelName) {
		this.smiles = smiles;
		this.lower = lower;
		this.upper = upper;
		this.predictionMidpoint = mp;
		this.confidence = confidence;
		this.modelName = modelName;
	}
	
	public RegressionResult(String smiles, CPRegressionPrediction res, double confidence, String modelName){
		this(smiles, res.getInterval(confidence).getInterval().lowerEndpoint(),res.getInterval(confidence).getInterval().upperEndpoint(),res.getY_hat(),confidence, modelName);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RegressionResult prediction = (RegressionResult) o;
		return Objects.equals(this.smiles, prediction.smiles) &&
				Objects.equals(this.lower, prediction.lower) &&
				Objects.equals(this.upper, prediction.upper) &&
				Objects.equals(this.predictionMidpoint, prediction.predictionMidpoint)&&
				Objects.equals(this.modelName, prediction.modelName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(smiles, lower, upper, predictionMidpoint, modelName);
	}


	@Override
	public String toString() {
		JsonObject resp = new JsonObject();
		resp.put("smiles", smiles);
		resp.put("lower", Utils.roundTo3digits(lower));
		resp.put("upper", Utils.roundTo3digits(upper));
		resp.put("predictionMidpoint", Utils.roundTo3digits(predictionMidpoint));
		resp.put("confidence", confidence);
		resp.put("modelName", modelName);
		return Jsoner.prettyPrint(resp.toJson());
	}


}
