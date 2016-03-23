package org.globaltester.scriptrunner.ui.commands;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.scriptrunner.TestResourceExecutor;

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
		
		TestResourceExecutor exec = getExecutor();
		
		try{
			return exec.execute(resources, event.getParameters());
		} catch (RuntimeException e) {
			GtUiHelper.openErrorDialog(shell, "Running failed: " + e.getMessage());
			return null;
		}
	}
	
	protected abstract TestResourceExecutor getExecutor();

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
