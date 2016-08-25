package org.globaltester.scriptrunner.ui.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.globaltester.scriptrunner.ui.TestResourceExecutorUi;
import org.osgi.framework.Bundle;

/**
 * Command implementation that shows Test resources using one of the available
 * {@link TestResourceExecutorUi} implementations
 * 
 * @author mboonk
 *
 */
public class ShowTests {


	public void show(List<IResource> resources) {
		TestResourceExecutorUi [] exec = getExecutorUis();
		for (TestResourceExecutorUi current : exec){
			if (current.canShow(resources)){
				current.show(resources);
				return;
			}
		}
	}
	
	public void execute(List<IResource> resources){
	}
	
	public static TestResourceExecutorUi [] getExecutorUis(){
		Bundle testmanagerBundle = Platform.getBundle("org.globaltester.testmanager.ui");
		Bundle testrunnerBundle = Platform.getBundle("org.globaltester.testrunner.ui");

		List<TestResourceExecutorUi> executorUis = new ArrayList<>();
		try {
			executorUis.add((TestResourceExecutorUi) testmanagerBundle.loadClass("org.globaltester.testmanager.ui.TestManagerExecutorUi").newInstance());
		} catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// Do nothing, this solution is to be replaced by using a service based approach
		}
		try {
			executorUis.add((TestResourceExecutorUi) testrunnerBundle.loadClass("org.globaltester.testrunner.ui.TestRunnerExecutorUi").newInstance());
		} catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// Do nothing, this solution is to be replaced by using a service based approach
		}
		return executorUis.toArray(new TestResourceExecutorUi [executorUis.size()]);
	}
}
