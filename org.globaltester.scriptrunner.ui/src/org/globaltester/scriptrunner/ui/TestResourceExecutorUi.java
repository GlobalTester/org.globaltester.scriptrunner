package org.globaltester.scriptrunner.ui;

import java.util.List;

import org.eclipse.core.resources.IResource;

public interface TestResourceExecutorUi {
	public void show(List<IResource> resources);

	public boolean canShow(List<IResource> resources);
}
