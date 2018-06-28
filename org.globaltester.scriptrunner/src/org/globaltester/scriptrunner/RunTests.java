package org.globaltester.scriptrunner;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.platform.ExecutionRequirementsException;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.sampleconfiguration.SampleConfigManager;
import org.globaltester.scriptrunner.TestExecutionCallback.UserNotificationEvent;
import org.osgi.framework.Bundle;

public class RunTests {

	GtRuntimeRequirements runtimReqs;

	public RunTests(GtRuntimeRequirements runtimeReqs) {
		this.runtimReqs = runtimeReqs;
	}

	/**
	 * @param resources
	 * @param callback
	 * @return true, iff an executor has been found and execution has been started
	 */
	public boolean execute(List<IResource> resources, TestExecutionCallback callback) {
		try {
			TestExecutor[] exec = getExecutors();
			for (TestExecutor current : exec) {
				if (current.canExecute(resources)) {
					current.execute(runtimReqs, resources, callback);
					return true;
				}
			}
		} catch (ExecutionRequirementsException e) { //FIXME AAF remove this if possible after refactoring
			UserNotificationEvent event = new UserNotificationEvent();
			event.message = e.getUserMessage();
			callback.submitEvent(event);
		}
		return false;
	}

	public TestExecutor[] getExecutors() {
		//FIXME AAC move this to testrunner ui bundle
		Bundle testmanagerBundle = Platform.getBundle("org.globaltester.testmanager.ui");
		Bundle testrunnerBundle = Platform.getBundle("org.globaltester.testrunner.ui");

		List<TestExecutor> executors = new ArrayList<>();

		try {
			executors.add((TestExecutor) testmanagerBundle
					.loadClass("org.globaltester.testmanager.ui.TestSetExecutor").newInstance());
		} catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// Do nothing, this solution is to be replaced by using a service based approach
		}
		try {
			executors.add((TestExecutor) testrunnerBundle
					.loadClass("org.globaltester.testrunner.ui.TestCampaignExecutor").newInstance());
		} catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// Do nothing, this solution is to be replaced by using a service based approach
		}
		return executors.toArray(new TestExecutor[executors.size()]);
	}
	
	public static SampleConfig getLastUsedSampleConfig() {
		String lastUsedProjectName = PreferenceHelper.getPreferenceValue(Activator.getContext().getBundle().getSymbolicName(), Activator.PREFERENCE_ID_LAST_USED_SAMPLE_CONFIG_PROJECT);
		return SampleConfigManager.get(lastUsedProjectName);
	}
}
