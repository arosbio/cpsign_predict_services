package com.arosbio.api.rest.predict;

import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.arosbio.impl.Predict;

public class TestStartupServer {

	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

	@Test
	public void testStartupPredictService() throws Exception {
		environmentVariables.set(Predict.LICENSE_FILE_ENV_VARIABLE, "/Users/staffan/git/cpsign_predict_services/classification_eclipse-project/src/test/resources/resources/cpsign-predict.license");
		environmentVariables.set(Predict.MODEL_FILE_ENV_VARIABLE, "/Users/staffan/git/cpsign_predict_services/classification_eclipse-project/src/test/resources/test_models/acp_class_liblinear-1.5.0.cpsign");

		//		Predict pred = new Predict();
		String SMILES =  "COc(c1)cccc1C#N";
		Response resp = Predict.doPredict(SMILES);
		System.err.println(resp.getEntity());
		System.err.println(resp);

		Response respImg = Predict.doPredictImage(SMILES, 400, 500, true, true);
		System.err.println(respImg);
		System.err.println(respImg.getEntity());
	}
}
