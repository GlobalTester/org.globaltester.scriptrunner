package org.globaltester.testrunner.testframework;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.ExpectedResult;
import org.globaltester.testspecification.testframework.TestStep;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public class TestStepExecution {

	private TestStep testStep;
	Result testStepResult;
	List<Result> expResultsExecutionResults;

	/**
	 * Constructor for new TestStepExecutionInstance
	 * @param step	TestStep this execution instance should execute
	 */
	public TestStepExecution(TestStep step) {
		testStep = step;
	}

	public void execute(ScriptRunner sr, Context cx, boolean forceExecution) {
		//log TestStep ID and Command
		TestLogger.info("TestStep "+ testStep.getId());
		Element commandElem = testStep.getCommand();
		String command = commandElem.getTextNormalize();
		Element commandTextElem = commandElem.getChild("Text", commandElem.getNamespace());
		if (commandTextElem != null) {
			command = command + commandTextElem.getTextNormalize();
		}
		TestLogger.info("Command: "+command);
		Element commandApduElem = commandElem.getChild("APDU", commandElem.getNamespace());
		if (commandApduElem != null) {
			TestLogger.info("APDU: \n"+commandApduElem.getTextNormalize());
		}
		
		//log TestStep descriptions
		Iterator<String> descrIter = testStep.getDescriptions().iterator();
		while (descrIter.hasNext()) {
			TestLogger.debug("   * "+descrIter.next());			
		}
		
		//init the executor with context and script runner
		TestStepExecutor stepExecutor = new TestStepExecutor(sr, cx);
		
		//execute the test step itself
		String techCommandCode = testStep.getTechnicalCommand();
		if ((techCommandCode != null) && (techCommandCode.trim().length() > 0)) {
			testStepResult = stepExecutor.execute(techCommandCode, testStep.getId()+" - Command");
		} else {
			//if no code can be executed the result of the step itself is always ok
			testStepResult = new Result();
		}
		
		//execute all ExpectedResults
		List<ExpectedResult> expResultDefs = testStep.getExpectedResults();
		expResultsExecutionResults = new LinkedList<Result>();
		for (Iterator<ExpectedResult> expResultIter = expResultDefs.iterator(); expResultIter
				.hasNext();) {
			ExpectedResult curResult = expResultIter.next();
			
			TestLogger.info("ExpectedResult " + curResult.getId() + " (TestStep "+ testStep.getId()+")");
			
			//log ExpectedResult descriptions
			descrIter = curResult.getDescriptions().iterator();
			while (descrIter.hasNext()) {
				TestLogger.debug("   * "+descrIter.next());			
			}
			
			//execute the current expected result
			Result curExecutionResult;
			String techResultCode = curResult.getTechnicalResult();
			if ((techResultCode != null) && (techResultCode.trim().length() > 0)) {
				curExecutionResult = stepExecutor.execute(techResultCode, testStep.getId()+" - Expected Result "+curResult.getId());
			} else {
				//if no code can be executed the result of the result is always ok
				curExecutionResult = new Result();
			}
			expResultsExecutionResults.add(curExecutionResult);
			
		}
	}

}