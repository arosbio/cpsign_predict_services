package utils;

import org.junit.Assert;
import org.junit.Test;

public class Utils {

	public static String VALID_LICENSE_PATH;
	public static String EXPIRED_LICENSE_PATH;
	public static String MODEL_PATH;
	
	static {
		EXPIRED_LICENSE_PATH = Empty.class.getResource("/resources/cpsign-old.license").getPath();
		VALID_LICENSE_PATH = Empty.class.getResource("/resources/cpsign-std.license").getPath();
		MODEL_PATH = Empty.class.getResource("/resources/test-model-1.5.0.cpsign").getPath();
	}
	
	public static String getPath(String relPath) {
		return Empty.class.getResource(relPath).getPath();
	}
	
	@Test
	public void testCheckValidURLs() {
		System.err.println("valid: "+VALID_LICENSE_PATH);
		System.err.println("exp: "+EXPIRED_LICENSE_PATH);
		System.err.println("model: "+MODEL_PATH);
	}

	public static void assertContainsIgnoreCase(String text, String... texts) {
		String lc = text.toLowerCase();
		for (String t: texts) {
			String tLC = t.toLowerCase();
			Assert.assertTrue(lc + " do not contain: [" + tLC + "]", lc.contains(tLC));
		}
	}
	
}
