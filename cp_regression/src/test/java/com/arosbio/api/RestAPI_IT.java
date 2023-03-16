package com.arosbio.api;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.port;
import static io.restassured.RestAssured.reset;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notANumber;
import static org.hamcrest.Matchers.notNullValue;

import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.arosbio.api.model.ServiceRunning;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.cliftonlabs.json_simple.JsonException;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import utils.Utils;

public class RestAPI_IT {

	@ClassRule
	public final static EnvironmentVariables env = new EnvironmentVariables();

	static final String EMPTY_v2000 = "\n" + 
			"JME 2021-07-13 Wed Sep 22 10:12:45 GMT+200 2021\n" + 
			"\n" + 
			"  0  0  0  0  0  0  0  0  0  0999 V2000\n" + 
			"M  END\n" + 
			"";

	static final String EMPTY_v3000 = "\n" + 
			"JME 2021-07-13 Wed Sep 22 10:08:16 GMT+200 2021\n" + 
			"\n" + 
			"  0  0  0  0  0  0  0  0  0  0999 V3000\n" + 
			"M  V30 BEGIN CTAB\n" + 
			"M  V30 COUNTS 0 0 0 0 0\n" + 
			"M  V30 BEGIN ATOM\n" + 
			"M  V30 END ATOM\n" + 
			"M  V30 BEGIN BOND\n" + 
			"M  V30 END BOND\n" + 
			"M  V30 END CTAB\n" + 
			"M  END\n" + 
			"";

	@BeforeClass
	public static void init() throws InterruptedException {
		// Set the base of the service in RestAssured
		baseURI = "http://localhost";
		port = 8080;
		basePath = "api/v2/";
	}

	@AfterClass
	public static void tareDown() {
		reset();
	}

	//// INFO STUFF

	@Test
	public void checkHealth() throws URISyntaxException {
		get("/health").then()
		.statusCode(200)
		.contentType(ContentType.JSON)
		.body("message", is(ServiceRunning.OK_MSG));

	}

	@Test
	public void testModelInfo() throws JsonException {
		Response r = get("/modelInfo");
		// OK Status
		r.then()
		.statusCode(200)
		.contentType(ContentType.JSON)
		.body("modelName", notNullValue())
		.body("version", notNullValue());
	}


	// PREDICTION STUFF
	static String TEST_SMILES = "CCCCCCC=O";

	@Test
	public void testPredictGET() throws JsonProcessingException {
		System.out.println(" =========== Running predictGet =========== ");


		Response resp = given().queryParam("confidence",.5).queryParam("molecule",TEST_SMILES).get("/predict");
		assertValidPredictionJSON(resp, TEST_SMILES,.5);
	}

	@Test
	public void testPredictGETInvalidConf() throws JsonProcessingException {
		System.out.println(" =========== Running predictGet - invalid conf =========== ");


		Response resp = given().queryParam("molecule",TEST_SMILES).queryParam("confidence",-1).get("/predict");
		resp.then()
		.statusCode(400)
		.body("code", is(400))
		.body("message", containsStringIgnoringCase("confidence"))
		.body("fields", hasSize(1))
		.body("fields", hasItem("confidence"));
	}

	@Test
	public void testPredictPOST() {
		System.out.println(" =========== Running predictPOST =========== ");

		Response resp = given().queryParam("confidence",.75).body(TEST_SMILES).contentType(ContentType.TEXT).post("/predict");
		assertValidPredictionJSON(resp, TEST_SMILES,.75);
	}

	@Test
	public void testPredictPOST_MDL() throws Exception {
		System.out.println(" =========== Running predictPOST MDL =========== ");

		String mdl = IOUtils.toString(new FileInputStream(Utils.getPath("/mdl_v2000.txt")), StandardCharsets.UTF_8);

		Response resp = given().queryParam("confidence",.9).body(mdl).post("/predict");
		assertValidPredictionJSON(resp, null,0.9);
	}

	private static void assertValidPredictionJSON(Response resp, String smiles, double conf) {
		resp.then()
		.statusCode(200)
		.body("smiles", smiles!=null? is(smiles) : allOf(notNullValue(),containsString("C"),containsString("N"),containsString("=")))
		.body("modelName", notNullValue())
		.body("lower", not(notANumber()))
		.body("upper", not(notANumber()))//typeCompatibleWith(Double.class))
		.body("predictionMidpoint",not(notANumber())) //typeCompatibleWith(Double.class))
		.body("confidence", is((float)conf));
	}


	@Test
	public void testPredictImageGET() {
		System.out.println(" =========== Running predictImageGet =========== ");

		Response resp = given().queryParam("confidence",.75).queryParam("molecule", TEST_SMILES).get("/predictImage");
		resp.then()
		.statusCode(200);
		byte[] imgBytes = resp.getBody().asByteArray();
		Assert.assertTrue(imgBytes.length>100);
	}

	@Test
	public void testPredictImagePOST() {
		System.out.println(" =========== Running predictImagePOST =========== ");

		Response resp = given().queryParam("confidence",.8).queryParam("title", "true").queryParam("imageWidth",300).body(TEST_SMILES).post("/predictImage"); //.contentType(ContentType.URLENC)
		resp.then()
		.statusCode(200);
		byte[] imgBytes = resp.getBody().asByteArray();
		Assert.assertTrue(imgBytes.length>100);

	}

	@Test
	public void testPredictImagePOST_MDL() throws Exception {
		System.out.println(" =========== Running predictImagePOST MDL =========== ");

		String mdl = IOUtils.toString(new FileInputStream(Utils.getPath("/mdl_v2000.txt")), StandardCharsets.UTF_8);

		Response resp = given().queryParam("confidence",.9).queryParam("title", "true").queryParam("imageWidth",300).body(mdl).post("/predictImage"); //.contentType(ContentType.URLENC)
		resp.then()
		.statusCode(200);
		Assert.assertTrue(resp.getBody().asByteArray().length>100);

		// Test empty MDL files
		Response respEmpty = given().queryParam("title", "true").queryParam("imageWidth",300).body(EMPTY_v2000).post("/predictImage");
		resp.then()
		.statusCode(200);
		Assert.assertTrue(respEmpty.getBody().asByteArray().length>100);

		// Test empty MDL files
		respEmpty = given().queryParam("title", "true").queryParam("imageWidth",300).body(EMPTY_v3000).post("/predictImage");
		resp.then()
		.statusCode(200);
		Assert.assertTrue(respEmpty.getBody().asByteArray().length>100);
	}



}
