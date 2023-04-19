package com.arosbio.api.predict;

import jakarta.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.arosbio.impl.Predict;
import com.arosbio.services.utils.Utils;

import utils.Utilities;


public class TestFailSetup {
	
	@Test
	public void testNoModel() throws Exception {
		// Clear system property so no valid model can be found 
		System.setProperty(Utils.MODEL_FILE_ENV_VARIABLE, "");
		
		Predict.init(); // force the init method to run, using the new env-setting
		Response resp = Predict.checkHealth();
		Assert.assertEquals(503, resp.getStatus());
		Utilities.assertContainsIgnoreCase(resp.getEntity().toString(), "model","re-deploy");
	}
	

}
