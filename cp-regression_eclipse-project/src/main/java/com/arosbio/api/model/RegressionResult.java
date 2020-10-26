package com.arosbio.api.model;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.arosbio.impl.Utils;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction.PredictedInterval;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegressionResult {
	
	@JsonProperty("smiles")
	@NotNull
	private String smiles;
	
	@JsonProperty("lower")
	private Double lower = null;

	@JsonProperty("upper")
	private Double upper = null;

	@JsonProperty("predictionMidpoint")
	@NotNull
	private Double predictionMidpoint = null;

	@JsonProperty("confidence")
	private Double confidence;
	
	@JsonProperty("modelName")
	@NotNull
	private String modelName = null;;

	public RegressionResult(String smiles, CPRegressionPrediction res, Double confidence, String modelName) {
		this.smiles = smiles;
		this.confidence = confidence;
		this.modelName = modelName;
		if (res!=null) {
			this.predictionMidpoint = res.getY_hat();
			// Upper / lower only accessible if confidence was given
			if (confidence != null) {
				PredictedInterval interval = res.getInterval(confidence);
				this.upper = interval.getInterval().upperEndpoint();
				this.lower = interval.getInterval().lowerEndpoint();
			}
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
