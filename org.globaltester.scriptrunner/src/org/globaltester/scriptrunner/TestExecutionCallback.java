package org.globaltester.scriptrunner;

import java.util.List;

import org.globaltester.base.PropertyElement;

/**
 * Callback interface used by TestExecutors to interact with the calling
 * instance.
 * 
 * @author amay
 *
 */
public interface TestExecutionCallback {

	public static class TestResult {
		public int testCases = 0;
		public int overallResult = 0;
		public SubTestResult [] subResults;
	}

	public static class SubTestResult {
		public String testCaseId;
		public String logFileName;
		public String resultString;
	}
	
	public static class Event{
	}
	
	public static class EventResult{
	}
	
	public static class UserNotificationEvent extends Event {
		public String message;
	}
	
	public static class UserQuestionEvent extends UserNotificationEvent {
		public String [] possibleResults;
		public List<PropertyElement> properties;
	}
	
	public static class UserQuestionEventResult extends EventResult {
		public int result;
	}
	
	TestExecutionCallback NULL_CALLBACK = new TestExecutionCallback () {

		@Override
		public void testExecutionFinished(TestResult result) {
			// ignore intentionally
		}

		@Override
		public EventResult submitEvent(Event event) {
			return new EventResult();
		}

	};

	/**
	 * This method is called when test execution is finished and informs the
	 * callback about the overall results.
	 * 
	 * @param result
	 */
	public void testExecutionFinished(TestResult result); // XXX return org.globaltester.testrunner.testframework.Result or something similar as parameter

	/**
	 * Submit an event to be processed by this callback.
	 * 
	 * @param event
	 * @return
	 */
	public EventResult submitEvent(Event event);

}
