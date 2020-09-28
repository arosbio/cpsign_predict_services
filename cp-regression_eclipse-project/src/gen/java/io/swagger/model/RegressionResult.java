package io.swagger.model;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.json.simple.JSONObject;

import com.arosbio.api.rest.predict.Utils;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RegressionResult {
	
	@JsonProperty("smiles")
	@ApiModelProperty(required = true, value = "SMILES string for the molecule used in the prediction", example="CCCCC=O")
	@NotNull
	private final String smiles;
	
	@JsonProperty("lower")
	@ApiModelProperty(value = "The lower range of the prediction value", required=true, example="1.755")
	private final Double lower;

	@JsonProperty("upper")
	@ApiModelProperty(value = "The upper range of the prediction value", required=true, example="2.571")
	private final Double upper;

	@JsonProperty("predictionMidpoint")
	@ApiModelProperty(
			value = "The predicted midpoint value, note that this is the  prediction given by the underlying SVM-models and  there is NO confidence assigned to this point value!", 
			required=true, example="2.163")
	private final Double predictionMidpoint;

	@JsonProperty("confidence")
	@ApiModelProperty(value = "The confidence of the prediction", required=true, example="0.8")
	private final Double confidence;
	
	@JsonProperty("modelName")
	@ApiModelProperty(required=true, value="Name of the model used for the prediction")
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
	public boolean equals(java.lang.Object o) {
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


	@SuppressWarnings("unchecked")
	@Override
	public String toString() {
		JSONObject resp = new JSONObject();
		resp.put("smiles", smiles);
		resp.put("lower", Utils.roundTo3digits(lower));
		resp.put("upper", Utils.roundTo3digits(upper));
		resp.put("predictionMidpoint", Utils.roundTo3digits(predictionMidpoint));
		resp.put("confidence", confidence);
		resp.put("modelName", modelName);
		return resp.toJSONString();
	}


}
