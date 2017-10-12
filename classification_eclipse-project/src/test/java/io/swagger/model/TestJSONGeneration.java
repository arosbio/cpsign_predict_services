package io.swagger.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class TestJSONGeneration {

	@Test
	public void testErrorJSON() {
		Error error = new Error(500, "error message");
		String golden 
			= "{\n  \"code\": 500,\n  \"message\": \"error message\"\n}";
		assertEquals(golden, error);
	}

	@Test
	public void testBadRequest() {
		BadRequestError error 
			= new BadRequestError( 400, 
					               "error message", 
					               Arrays.asList("smiles") );
		String golden
			= "{\n"
			+ "  \"code\": 400,\n"
			+ "  \"message\": \"error message\",\n"
			+ "  \"fields\": [\n"
			+ "    \"smiles\"\n"
			+ "  ]\n"
			+ "]\n";
		assertEquals(golden, error.toString());
	}
	
	@Test
	public void testClassification() {
		List<PValueMapping> mappings = new ArrayList<PValueMapping>();
		for (String s : new String[] {"mutagen", "nonmutagen"}) {
			mappings.add( new PValueMapping(s, 0.5) );
		}
		Classification classification = new Classification(mappings, "CCC");
		String golden
			= "{\n"
			+ "  \"smiles\": \"string\",\n"
			+ "  \"prediction\": [\n"
			+ "    {\n"
			+ "      \"label\": \"mutagen\",\n"
			+ "      \"pValue\": 0.5\n"
			+ "    }\n"
			+ "    {\n"
			+ "      \"label\": \"nonmutagen\",\n"
			+ "    }\n"
			+ "  ]\n"
			+ "}\n";
		assertEquals(golden, classification.toString());
	}
}
