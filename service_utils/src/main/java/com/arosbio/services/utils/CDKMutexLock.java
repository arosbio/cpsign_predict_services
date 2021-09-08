package com.arosbio.services.utils;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A serializing mechanism that works by having an internal lock. Whenever some piece of 
 * code need to use CDK or CPSign, require the lock and release after finished using CKD/CPSign.
 * @author staffan
 *
 */
public class CDKMutexLock {
	
	private final static Lock lock = new ReentrantLock();
	
	public static void requireLock(){
		lock.lock();
	}
	
	public static void releaseLock() {
		try {
			lock.unlock();
		} catch (IllegalMonitorStateException e) {
			// Didn't hold the lock, thus we don't need to handle this
		}
	}

}
