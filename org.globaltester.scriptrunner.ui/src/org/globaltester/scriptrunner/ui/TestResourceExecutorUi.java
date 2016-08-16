package org.globaltester.scriptrunner.ui;

import java.util.List;

import org.eclipse.core.resources.IResource;

/**
 * Implementations of this interface are used to display execution results of workspace
 * resources as test cases.
 * 
 * @author mboonk
 *
 */
public interface TestResourceExecutorUi {
	public void show(List<IResource> resources);

	public boolean canShow(List<IResource> resources);
}
