package org.globaltester.scriptrunner;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Implementations of this interface are used for execution of workspace
 * resources as test cases.
 * <p/>
 * Parallel execution is not supported at the moment. Thus every implementing
 * class should respect the locking imposed by {@link TestResourceExecutorLock}
 * 
 * @author mboonk
 *
 */
public abstract class TestExecutor {

	public static final String FAMILY = "GT_TESTEXEC";

	/**
	 * @param resources
	 *            the resources to be checked
	 * @return true, iff the given resources can be executed by this executor
	 */
	public abstract boolean canExecute(List<IResource> resources);

	/**
	 * Execute the given resources.
	 * 
	 * @param runtimeRequirements
	 *            this provides the test environment
	 * @param resources
	 *            the resources to be executed
	 * @param callback
	 *            a callback object for communication with the caller of this
	 *            method
	 * @return
	 */
	public Object execute(GtRuntimeRequirements runtimeRequirements, List<IResource> resources,
			TestExecutionCallback callback) {

		addRuntimeRequirements(runtimeRequirements);

		Job job = getExecutionJob(resources, runtimeRequirements, callback, TestExecutor.FAMILY);
		job.setUser(true);
		job.schedule();

		return null;

	}

	/**
	 * Create and return the Job used for actual execution. The returned Job
	 * will be scheduled by the caller but must guarantee to send a single
	 * result to the given {@link TestExecutionCallback}
	 * 
	 * @param resources
	 * @param runtimeRequirements
	 * @param callback
	 * @param family
	 * @return
	 */
	protected abstract Job getExecutionJob(List<IResource> resources, GtRuntimeRequirements runtimeRequirements,
			TestExecutionCallback callback, Object family);

	/**
	 * This abstract method allows implementations to provide additional
	 * {@link GtRuntimeRequirements} for this execution
	 * 
	 * @param runtimeRequirements
	 */
	protected abstract void addRuntimeRequirements(GtRuntimeRequirements runtimeRequirements);
}
