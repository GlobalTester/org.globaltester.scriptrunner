package org.globaltester.scriptrunner;

import org.globaltester.base.UserInteraction;

/**
 * This provides {@link UserInteraction} objects for test case executions.
 * @author mboonk
 *
 */
public interface UserInteractionProvider extends RuntimeRequirementsProvider {
	/**
	 * @return a {@link UserInteraction} object
	 */
	public UserInteraction getUserInteraction();
}
