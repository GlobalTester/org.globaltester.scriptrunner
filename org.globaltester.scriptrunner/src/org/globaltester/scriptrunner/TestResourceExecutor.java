package org.globaltester.scriptrunner;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;

/**
 * Implementations of this interface are used for execution of workspace
 * resources as test cases.
 * 
 * @author mboonk
 *
 */
public interface TestResourceExecutor {
	public boolean canExecute(List<IResource> resources);
	public Object execute(List<IResource> resources, Map<?, ?> map);
}
