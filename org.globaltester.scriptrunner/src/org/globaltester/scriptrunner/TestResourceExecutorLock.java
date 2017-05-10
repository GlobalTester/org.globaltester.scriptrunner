package org.globaltester.scriptrunner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * To ensure that only one TestResourceExecution is running at any given time
 * this class provides a lock that must be acquired before and hold during test
 * execution.
 */
public class TestResourceExecutorLock {

	private static Lock lock = null;

	// hide implicit public constructor
	private TestResourceExecutorLock() {
	}

	public static synchronized Lock getLock() {
		if (lock == null) {
			lock = new ReentrantLock();
		}
		return lock;
	}

	/**
	 * In order to allow parallel execution under special circumstances (e.g.
	 * within crossover integration tests) it is possible to change the
	 * underlying lock implementation through this method.
	 * 
	 * @param newLock
	 *            new Lock instance to be used
	 * @return true iff the lock was set successfully
	 */
	public static synchronized boolean setLock(Lock newLock) {
		if (lock == null) {
			lock = newLock;
			return true;
		}
		return false;
	}

}
