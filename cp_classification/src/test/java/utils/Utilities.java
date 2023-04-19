package utils;

import org.junit.Assert;

public class Utilities {

	public static String MODEL_PATH;
	
	static {
		MODEL_PATH = Utilities.class.getResource("/test-model.cpsign").getPath();
	}
	
	public static String getPath(String relPath) {
		return Utilities.class.getResource(relPath).getPath();
	}

	public static void assertContainsIgnoreCase(String text, String... texts) {
		String lc = text.toLowerCase();
		for (String t: texts) {
			String tLC = t.toLowerCase();
			Assert.assertTrue(lc + " do not contain: [" + tLC + "]", lc.contains(tLC));
		}
	}
	
}
