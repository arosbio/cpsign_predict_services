package com.arosbio.api.model;

import java.util.Objects;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import com.arosbio.commons.MathUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PValueMapping {
	
	@JsonProperty()
	private final String label;

	@JsonProperty()
	@NotNull
	@DecimalMin("0.0") @DecimalMax("1.0")
	private final Double pValue;

	public PValueMapping(String label, Double pvalue) {
		this.label = label;
		this.pValue = MathUtils.roundTo3significantFigures(pvalue);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PValueMapping pvalueMapping = (PValueMapping) o;
		return Objects.equals(this.label, pvalueMapping.label) &&
				Objects.equals(this.pValue, pvalueMapping.pValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, pValue);
	}

}