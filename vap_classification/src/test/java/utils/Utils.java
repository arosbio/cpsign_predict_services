package utils;

import org.junit.Assert;

public class Utils {
	
	public static String getPath(String relPath) {
		return Utils.class.getResource(relPath).getPath();
	}

	public static void assertContainsIgnoreCase(String text, String... texts) {
		String lc = text.toLowerCase();
		for (String t: texts) {
			String tLC = t.toLowerCase();
			Assert.assertTrue(lc + " do not contain: [" + tLC + "]", lc.contains(tLC));
		}
	}
	
}
