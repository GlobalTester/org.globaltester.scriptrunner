package org.globaltester.scriptrunner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestResourceExecutorLock {

	/**
	 * Ensure that only one TestResourceExecution is running at any given time.
	 * This is required for the moment but should be removed in the future as soon as parallel execution is supported
	 */
	public static Lock lock = new ReentrantLock();

}
