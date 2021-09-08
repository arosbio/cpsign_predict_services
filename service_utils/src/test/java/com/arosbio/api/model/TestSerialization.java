package com.arosbio.api.model;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import suites.classes.UnitTest;

@Category(UnitTest.class)
public class TestSerialization {
	
	@Test
	public void testError() {
		ErrorResponse err = new ErrorResponse(500, "some msg");
		String shouldBeJSON = err.toString();
		try {
			JsonObject json = (JsonObject)Jsoner.deserialize(shouldBeJSON);
			Assert.assertTrue(json.size() == 2);
			Assert.assertEquals(json.get("message"), "some msg");
			Assert.assertEquals(Integer.parseInt(json.get("code").toString()), 500);
		} catch (JsonException e) {
			Assert.fail("Failed to convert to json: " + e.getMessage());
		}
	}

}
