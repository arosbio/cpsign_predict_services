package com.arosbio.api.predict;

import javax.ws.rs.core.Response;

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
	public void testNoLicenseOrModel() throws Exception {
		env.clear(Predict.MODEL_FILE_ENV_VARIABLE);
		Predict.init();
		Response resp = Predict.checkHealth();
		Assert.assertEquals(503, resp.getStatus());
		Utils.assertContainsIgnoreCase(resp.getEntity().toString(), "license","re-deploy");
	}
	
	@Test
	public void testNoModel() throws Exception {
//		env.set(Predict.LICENSE_FILE_ENV_VARIABLE, null);
		env.clear(Predict.MODEL_FILE_ENV_VARIABLE);
		Predict.init();
		Response resp = Predict.checkHealth();
		Assert.assertEquals(503, resp.getStatus());
		Utils.assertContainsIgnoreCase(resp.getEntity().toString(), "model","re-deploy");
	}
	

}
