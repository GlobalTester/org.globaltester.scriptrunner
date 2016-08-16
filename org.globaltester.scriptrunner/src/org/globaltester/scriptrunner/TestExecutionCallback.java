package org.globaltester.scriptrunner;

/**
 * Callback interface used by TestExceutors to interact with the calling
 * instance.
 * 
 * @author amay
 *
 */
public interface TestExecutionCallback {

	public static class TestResult {
		public TestResult(int testCases, int failures, int warnings) {
			this.testCases = testCases;
			this.failures = failures;
			this.warnings = warnings;
		}
		public int testCases = 0;
		public int failures = 0;
		public int warnings = 0;

	}

	/**
	 * This method is called when test execution is finished and informs the
	 * callback about the overall results.
	 * 
	 * @param result
	 */
	public void testExecutionFinished(TestResult result); // XXX return org.globaltester.testrunner.testframework.Result or something similar as parameter

}
