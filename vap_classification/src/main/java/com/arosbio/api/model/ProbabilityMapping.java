package com.arosbio.api.model;

import java.util.Objects;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import com.arosbio.commons.MathUtils;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ProbabilityMapping {
	@JsonProperty("label")
	@NotNull
	private final String label;

	@JsonProperty("probability")
	@NotNull
	@DecimalMin("0") @DecimalMax("1")
	private final Double pValue;

	public ProbabilityMapping(String label, Double pvalue) {
		this.label = label;
		this.pValue = MathUtils.roundTo3significantFigures(pvalue);
	}
	
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProbabilityMapping pvalueMapping = (ProbabilityMapping) o;
		return Objects.equals(this.label, pvalueMapping.label) &&
				Objects.equals(this.pValue, pvalueMapping.pValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, pValue);
	}

}