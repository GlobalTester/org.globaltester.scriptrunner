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
		
		/**
		 * Creates a new Testersult
		 *  
		 * @param nrOfElements number of elements executed
		 * @param result {@see Result#Status}
		 */
		public TestResult(int nrOfElements, int result) {
			this.testCases = nrOfElements;
			this.overallResult = result;
		}
		
		public int testCases = 0;
		public int overallResult = 0;

	}

	TestExecutionCallback NULL_CALLBACK = new TestExecutionCallback () {
		@Override
		public void testExecutionFinished(TestResult result) {
			// intentionally ignore
		}
	};

	/**
	 * This method is called when test execution is finished and informs the
	 * callback about the overall results.
	 * 
	 * @param result
	 */
	public void testExecutionFinished(TestResult result); // XXX return org.globaltester.testrunner.testframework.Result or something similar as parameter

}
