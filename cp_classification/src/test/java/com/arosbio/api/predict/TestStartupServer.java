package com.arosbio.api.predict;

import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.arosbio.api.model.ClassificationResult;
import com.arosbio.impl.Predict;

import jakarta.ws.rs.core.Response;
import utils.Utils;

public class TestStartupServer {

	@Rule
	public final EnvironmentVariables env = new EnvironmentVariables();

	@Test
	public void testStartupPredictService() throws Exception {
		env.set(Predict.MODEL_FILE_ENV_VARIABLE, Utils.MODEL_PATH);
		Predict.init(); // force the init method to run, using the new env-setting
		String SMILES =  "COc(c1)cccc1C#N";
		Response resp = Predict.doPredict(SMILES);
		ClassificationResult result = (ClassificationResult) resp.getEntity();
		Assert.assertEquals(SMILES, result.smiles);
		Assert.assertEquals(200, resp.getStatus());
		// System.err.println();
		// System.err.println(resp);

		Response respImg = Predict.doPredictImage(SMILES, 400, 500, true, true, true);
		
		Assert.assertEquals("should be OK", 200, respImg.getStatus());
		Assert.assertTrue("the result should be an image stream",respImg.getEntity() instanceof ByteArrayInputStream);
		
		// System.err.println(respImg);
		// System.err.println(respImg.getEntity());
	}

}
