package com.arosbio.api;

import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;
import static org.hamcrest.CoreMatchers.equalTo;

import org.hamcrest.CoreMatchers;

public class IT_PredictAPIFailedSetup {
	
	@BeforeClass
	public static void init() {
		RestAssured.baseURI = "http://localhost:8080/api/v2";
	}
	
	
	@Test
	public void testHealth() {
		RestAssured.get("/health").then().statusCode(503).body("code", equalTo(503)).body("message", CoreMatchers.startsWith("No license"));
		RestAssured.head("/health").then().statusCode(503);
	}
	

}
