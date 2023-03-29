package com.arosbio.api.predict;

import jakarta.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.arosbio.impl.Predict;

import utils.Utils;


public class TestFailSetup {
	
	@Rule
	public final EnvironmentVariables env = new EnvironmentVariables();
	
	@Test
	public void testNoModel() throws Exception {
		env.clear(Predict.MODEL_FILE_ENV_VARIABLE);
		Predict.init(); // force the init method to run, using the new env-setting
		Response resp = Predict.checkHealth();
		Assert.assertEquals(503, resp.getStatus());
		Utils.assertContainsIgnoreCase(resp.getEntity().toString(), "model","re-deploy");
	}
	

}
