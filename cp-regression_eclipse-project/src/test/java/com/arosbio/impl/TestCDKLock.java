package com.arosbio.impl;

import org.junit.Test;

public class TestCDKLock {
	
	@Test
	public void testReleaseWhenNotLocked() {
		CDKMutexLock.releaseLock();
	}

}
