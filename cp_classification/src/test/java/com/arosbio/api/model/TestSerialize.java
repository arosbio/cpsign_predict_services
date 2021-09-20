package com.arosbio.api.model;

import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSerialize {
	
	@Test
	public void testClassificationJSON() throws JsonProcessingException {
		ClassificationResult res = new ClassificationResult(Arrays.asList(new PValueMapping("A", .021), new PValueMapping("B", .52)), "cccc", "NaN");
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(res);
		System.err.println(json);
	}

}
