package com.arosbio.services.utils;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.arosbio.api.model.BadRequestError;

import jakarta.ws.rs.core.Response;


public class TestUtilsClass {

	@Test
	public void testValidateImageSizeOK() {
		Assert.assertNull(Utils.validateImageSize(Utils.MIN_IMAGE_SIZE, Utils.MIN_IMAGE_SIZE));
		Assert.assertNull(Utils.validateImageSize(Utils.DEFAULT_IMAGE_WH, Utils.DEFAULT_IMAGE_WH));
		Assert.assertNull(Utils.validateImageSize(Utils.MAX_IMAGE_SIZE, Utils.MAX_IMAGE_SIZE));
	}
	
	@Test
	public void testValidateImageSizeWidth() {
		// Invalid W - too small
		assertBadReq(Utils.validateImageSize(-1, 400), true, false);
		assertBadReq(Utils.validateImageSize(Utils.MIN_IMAGE_SIZE - 1, Utils.DEFAULT_IMAGE_WH), 
				true, false);
		// Too big
		assertBadReq(Utils.validateImageSize(10009, 400), true, false);
		assertBadReq(Utils.validateImageSize(Utils.MAX_IMAGE_SIZE + 1, Utils.DEFAULT_IMAGE_WH), 
				true, false);
	}
	
	@Test
	public void testValidateImageSizeHeight() {
		// Invalid H - too small
		Response r = Utils.validateImageSize(400, 0);
		assertBadReq(r, false, true);
		assertBadReq(Utils.validateImageSize(Utils.DEFAULT_IMAGE_WH, Utils.MIN_IMAGE_SIZE - 1), 
				false,true);
		// Too big
		assertBadReq(Utils.validateImageSize(400, Utils.MAX_IMAGE_SIZE+1), 
				false,true);
	}
	
	@Test
	public void testValidateImageSizeWidthAndHeight() {
		// too small
		assertBadReq(Utils.validateImageSize(-1, -1), 
				true, true);
		assertBadReq(Utils.validateImageSize(Utils.MIN_IMAGE_SIZE - 1, Utils.MIN_IMAGE_SIZE-1), 
				true, true);
		// Too big
		assertBadReq(Utils.validateImageSize(Utils.MAX_IMAGE_SIZE + 1, Utils.MAX_IMAGE_SIZE + 1), 
				true, true);
		
		// One big and one small
		assertBadReq(Utils.validateImageSize(Utils.MAX_IMAGE_SIZE + 1, Utils.MIN_IMAGE_SIZE - 1), 
				true, true);
		assertBadReq(Utils.validateImageSize(Utils.MIN_IMAGE_SIZE - 1, Utils.MAX_IMAGE_SIZE + 1), 
				true, true);
	}
	
	private static void assertBadReq(Response r, boolean invalidW,boolean invalidH) {
		Assert.assertNotNull(r);
		BadRequestError bre = (BadRequestError) r.getEntity();
		String msg = bre.getMessage().toLowerCase();
		if (invalidW) {
			Assert.assertTrue(msg.contains("imagewidth"));
			assertContainsField(bre.getFields(), "imageWidth");
		}
		if (invalidH) {
			Assert.assertTrue(msg.contains("imageheight"));
			assertContainsField(bre.getFields(), "imageHeight");
		}
	}
	
	private static boolean assertContainsField(List<String> fields, String txt) {
		for (String l : fields) {
			if (l.equalsIgnoreCase(txt))
				return true;
		}
		Assert.fail("Fields did not contain text: " + txt);
		return false;
	}
	
	
}
