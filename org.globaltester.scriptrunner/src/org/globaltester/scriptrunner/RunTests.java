package org.globaltester.scriptrunner;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.osgi.framework.Bundle;

public class RunTests {
	
	SampleConfig config;
	
	public RunTests(SampleConfig config){
		this.config = config;
	}
	
	public void execute(List<IResource> resources){
		TestResourceExecutor [] exec = getExecutors();
		for (TestResourceExecutor current : exec){
			if (current.canExecute(resources)){
				current.execute(config, resources);
				return;
			}
		}
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
