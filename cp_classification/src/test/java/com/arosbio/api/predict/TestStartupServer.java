package com.arosbio.api.predict;

import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.arosbio.impl.Predict;

import utils.Utils;

public class TestStartupServer {

	@Rule
	public final EnvironmentVariables env = new EnvironmentVariables();

	@Test
	public void testStartupPredictService() throws Exception {
		env.set(Predict.LICENSE_FILE_ENV_VARIABLE, Utils.VALID_LICENSE_PATH);
		env.set(Predict.MODEL_FILE_ENV_VARIABLE, Utils.MODEL_PATH);

		//		Predict pred = new Predict();
		String SMILES =  "COc(c1)cccc1C#N";
		Response resp = Predict.doPredict(SMILES);
		System.err.println(resp.getEntity());
		System.err.println(resp);

		Response respImg = Predict.doPredictImage(SMILES, 400, 500, true, true);
		System.err.println(respImg);
		System.err.println(respImg.getEntity());
	}

//	@Test
//	public void testOldLicense() throws Exception {
//		env.set(Predict.LICENSE_FILE_ENV_VARIABLE, Utils.EXPIRED_LICENSE_PATH);
//		
//		RestAssured.baseURI = "http://localhost:8080/api/v2";
//
//		RestAssured.get("/health").then().statusCode(503).body("code", equalTo(503)).body("message", CoreMatchers.startsWith("No license"));
//		RestAssured.head("/health").then().statusCode(503);
//	}
}
