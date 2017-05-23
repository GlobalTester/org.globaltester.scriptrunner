package org.globaltester.scriptrunner;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class RunTests {
	
	GtRuntimeRequirements runtimReqs;
	
	public RunTests(GtRuntimeRequirements runtimeReqs){
		this.runtimReqs = runtimeReqs;
	}
	
	/**
	 * @param resources
	 * @param callback
	 * @return true, iff an executor has been found and execution has been
	 *         started
	 */
	public boolean execute(List<IResource> resources, TestExecutionCallback callback){
		TestResourceExecutor [] exec = getExecutors();
		for (TestResourceExecutor current : exec){
			if (current.canExecute(resources)){
				current.execute(runtimReqs, resources, callback);
				return true;
			}
		}
		return false;
	}
	
	public TestResourceExecutor [] getExecutors(){
		Bundle testmanagerBundle = Platform.getBundle("org.globaltester.testmanager.ui");
		Bundle testrunnerBundle = Platform.getBundle("org.globaltester.testrunner.ui");

		List<TestResourceExecutor> executors = new ArrayList<>();
		
		try {
			executors.add((TestResourceExecutor) testmanagerBundle.loadClass("org.globaltester.testmanager.ui.TestManagerExecutor").newInstance());
		} catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// Do nothing, this solution is to be replaced by using a service based approach
		}
		try {
			executors.add((TestResourceExecutor) testrunnerBundle.loadClass("org.globaltester.testrunner.ui.TestRunnerExecutor").newInstance());
		} catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// Do nothing, this solution is to be replaced by using a service based approach
		}
		return executors.toArray(new TestResourceExecutor [executors.size()]);
	}
}
