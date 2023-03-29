package com.arosbio.api.model;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class TestSerialize {
	
	@Test
	public void testClassificationJSON() throws JsonProcessingException, JsonException {
		String smiles = "cccc";
		String mName = "NaN";
		ClassificationResult res = new ClassificationResult(
			Arrays.asList(new PValueMapping("A", .021),new PValueMapping("B", .52)), 
			smiles, 
			mName);
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(res);
		JsonObject json = (JsonObject) Jsoner.deserialize(jsonString);
		Assert.assertTrue(json.containsKey("smiles"));
		Assert.assertEquals(smiles,json.get("smiles"));
		Assert.assertTrue(json.containsKey("modelName"));
		Assert.assertEquals(mName,json.get("modelName"));
		// System.err.println(jsonString);
	}

}
