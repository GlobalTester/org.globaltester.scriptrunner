package org.globaltester.scriptrunner;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.globaltester.sampleconfiguration.SampleConfig;

/**
 * Implementations of this interface are used for execution of workspace
 * resources as test cases.
 * 
 * @author mboonk
 *
 */
public interface TestResourceExecutor {
	public boolean canExecute(List<IResource> resources);
	public Object execute(SampleConfig config, List<IResource> resources, TestExecutionCallback callback);
}
