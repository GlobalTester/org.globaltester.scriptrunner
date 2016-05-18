package org.globaltester.scriptrunner.ui.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.scriptrunner.TestResourceExecutor;
import org.osgi.framework.Bundle;

public abstract class RunTestCommandHandler extends AbstractHandler {
	/**
	 * sets up environment, e.g. prepares settings for debugging threads and
	 * launches and starts them, dependent on what is currently activated and
	 * needed.
	 * 
	 * @param event
	 *            which triggers the handler and delivers information on
	 *            selected resource etc.
	 * @param envSettings used for adding or retrieving environment information
	 * @throws RuntimeException in case of errors
	 */
	protected void setupEnvironment(ExecutionEvent event, Map<String, Object> envSettings)  throws RuntimeException {
		// does nothing special here; can be overridden by derived classes
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}
		
		ISelection iSel = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		LinkedList<IResource> resources = GtUiHelper.getSelectedIResources(iSel, IResource.class);
		
		
		if (resources.size() == 0){
			//try to get file from editor
			IFile file = getFileFromEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart());
			if (file != null){
				resources.add(file);
			}
		}
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		if (resources.size() == 0){
			GtUiHelper.openErrorDialog(shell, "Select executable files or an editor for execution of test cases.");
			return null;
		}
		
		TestResourceExecutor [] exec = getExecutors();
		
		try{
			//clean the result view
			IViewPart resultView = null;
			IViewReference viewReferences[] = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().getViewReferences();
			for (int i = 0; i < viewReferences.length; i++) {
				if ("org.globaltester.testmanager.views.ResultView".equals(viewReferences[i].getId())) {
					resultView = viewReferences[i].getView(false);
				}
			}
			if(resultView != null) {
				try {
					Bundle testmanagerBundle = Platform.getBundle("org.globaltester.testmanager");
					Class<?> resultViewClass = testmanagerBundle.loadClass("org.globaltester.testmanager.views.ResultView");
					Method reset = resultViewClass.getDeclaredMethod("reset");
					reset.invoke(resultView);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			//execute test(s)
			for (TestResourceExecutor current : exec){
				if (current.canExecute(resources)){
					return current.execute(resources, event.getParameters());
				}
			}
		} catch (RuntimeException e) {
			GtUiHelper.openErrorDialog(shell, "Running failed: " + e.getMessage());
			return null;
		}
		GtUiHelper.openErrorDialog(shell, "Running failed, no applicable executors found for this selection");
		return null;
	}
	
	protected TestResourceExecutor [] getExecutors(){
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

	protected IFile getFileFromEditor(IWorkbenchPart activePart){
		if (activePart instanceof EditorPart){
			EditorPart editor = (EditorPart) activePart;
			if (editor.getEditorInput() instanceof IPathEditorInput ){
				IPathEditorInput input = (IPathEditorInput) editor.getEditorInput();
				return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(input.getPath());
			}
		}
		return null;
	};
}
