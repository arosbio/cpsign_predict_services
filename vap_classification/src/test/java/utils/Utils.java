package utils;

import org.junit.Assert;

public class Utils {

	public static String VALID_LICENSE_PATH;
	public static String MODEL_PATH;
	
	static {
		VALID_LICENSE_PATH = Utils.class.getResource("/resources/cpsign.license").getPath();
		MODEL_PATH = Utils.class.getResource("/resources/test-model.cpsign").getPath();
	}
	
	public static String getPath(String relPath) {
		return Utils.class.getResource(relPath).getPath();
	}
	
//	@Test
	public void testCheckValidURLs() {
		System.err.println("valid: "+VALID_LICENSE_PATH);
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
