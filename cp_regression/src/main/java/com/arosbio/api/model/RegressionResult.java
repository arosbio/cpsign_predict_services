package com.arosbio.api.model;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.arosbio.ml.cp.CPRegressionPrediction;
import com.arosbio.ml.cp.CPRegressionPrediction.PredictedInterval;
import com.arosbio.services.utils.Utils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegressionResult {
	
	@JsonProperty("smiles")
	@NotNull
	private final String smiles;
	
	@JsonProperty("lower")
	private final Double lower;

	@JsonProperty("upper")
	private final Double upper;

	@JsonProperty("predictionMidpoint")
	@NotNull
	private final Double predictionMidpoint;

	@JsonProperty("confidence")
	private final Double confidence;
	
	@JsonProperty("modelName")
	@NotNull
	private final String modelName;

	public RegressionResult(String smiles, CPRegressionPrediction res, Double confidence, String modelName) {
		this.smiles = smiles;
		this.confidence = confidence;
		this.modelName = modelName;
		if (res!=null) {
			this.predictionMidpoint = Utils.roundTo3digits(res.getY_hat());
			// Upper / lower only accessible if confidence was given
			if (confidence != null) {
				PredictedInterval interval = res.getInterval(confidence);
				this.upper = Utils.roundTo3digits(interval.getInterval().upperEndpoint());
				this.lower = Utils.roundTo3digits(interval.getInterval().lowerEndpoint());
			} else {
				this.lower = null;
				this.upper = null;
			}
		} else {
			this.predictionMidpoint = null;
			this.lower = null;
			this.upper = null;
		}
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
