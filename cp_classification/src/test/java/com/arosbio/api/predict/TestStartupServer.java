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
		env.set(Predict.MODEL_FILE_ENV_VARIABLE, Utils.MODEL_PATH);

		//		Predict pred = new Predict();
		String SMILES =  "COc(c1)cccc1C#N";
		Response resp = Predict.doPredict(SMILES);
		System.err.println(resp.getEntity());
		System.err.println(resp);

		Response respImg = Predict.doPredictImage(SMILES, 400, 500, true, true, true);
		System.err.println(respImg);
		System.err.println(respImg.getEntity());
	}

}
