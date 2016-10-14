package org.globaltester.scriptrunner;

import java.util.List;

import org.eclipse.core.resources.IResource;

/**
 * Implementations of this interface are used for execution of workspace
 * resources as test cases.
 * 
 * @author mboonk
 *
 */
public interface TestResourceExecutor {
	
	/**
	 * @param resources
	 *            the resources to be checked
	 * @return true, iff the given resources can be executed by this executor
	 */
	public boolean canExecute(List<IResource> resources);
	
	/**
	 * Execute the given resources.
	 * 
	 * @param requirementsProvider
	 *            this provides the test environment
	 * @param resources
	 *            the resources to be executed
	 * @param callback
	 *            a callback object for communication with the caller of this
	 *            method
	 * @return
	 */
	public Object execute(RuntimeRequirementsProvider requirementsProvider, List<IResource> resources, TestExecutionCallback callback);
}
