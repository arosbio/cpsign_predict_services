package com.arosbio.services.utils;

import org.junit.Test;

import com.arosbio.services.utils.CDKMutexLock;

public class TestCDKLock {
	
	@Test
	public void testReleaseWhenNotLocked() {
		CDKMutexLock.releaseLock();
	}

}
