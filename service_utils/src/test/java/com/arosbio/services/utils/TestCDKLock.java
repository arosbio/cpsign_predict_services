package com.arosbio.services.utils;

import org.junit.Test;

public class TestCDKLock {
	
	@Test
	public void testReleaseWhenNotLocked() {
		CDKMutexLock.releaseLock();
	}

}
