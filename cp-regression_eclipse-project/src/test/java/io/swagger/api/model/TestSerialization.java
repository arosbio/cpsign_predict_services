package io.swagger.api.model;

import org.junit.Test;

public class TestSerialization {
	
	@Test
	public void testError() {
		ErrorResponse err = new ErrorResponse(500, "some msg");
		System.err.println(err);
	}

}
