package org.globaltester.scriptrunner.ui.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.base.ui.UserInteractionImpl;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.sampleconfiguration.SampleConfigManager;
import org.globaltester.sampleconfiguration.ui.SampleConfigSelectorDialog;
import org.globaltester.scriptrunner.Activator;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.RunTests;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.scriptrunner.TestResourceExecutorLock;
import org.globaltester.scriptrunner.ui.SampleConfigDialogCanceledException;
import org.globaltester.scriptrunner.ui.SampleConfigSelectionException;

public abstract class RunTestCommandHandler extends AbstractHandler {
	private boolean ignoreDirty = false;
	
	private List<IResource> resources;
	
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
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		if (TestResourceExecutorLock.getLock().tryLock()){
			TestResourceExecutorLock.getLock().unlock();
		} else {
			GtUiHelper.openErrorDialog(shell, "Already a TestExecution running. Please wait until that other execution has finished.");
			return null;
		}
		
		// check for dirty files and save them
		if ((!isIgnoreDirty()) && (!PlatformUI.getWorkbench().saveAllEditors(true))) {
			setIgnoreDirty(false);
			return null;
		}
		
		resources = createResourceList();
		
		if (resources.isEmpty()){
			GtUiHelper.openErrorDialog(shell, "Select executable files or an editor for execution of test cases.");
			return null;
		}
		
		try{
			new ShowTests().show(resources);
			
			SampleConfig config = getSampleConfig(event);
			
			if (config == null){
				GtUiHelper.openErrorDialog(shell, "Running failed: No sample config could be determined");
				return null;
			}
			
			PreferenceHelper.setPreferenceValue(Activator.getContext().getBundle().getSymbolicName(), Activator.PREFERENCE_ID_LAST_USED_SAMPLE_CONFIG_PROJECT, config.getName());
			
			GtRuntimeRequirements runtimeReqs = new GtRuntimeRequirements(new UserInteractionImpl(), config);
			
			if (!new RunTests(runtimeReqs).execute(resources, TestExecutionCallback.NULL_CALLBACK)){
				GtUiHelper.openErrorDialog(shell, "Running failed: No valid execution engine found for your selection.");
			}
			return null;
		} catch (RuntimeException e) {
			GtUiHelper.openErrorDialog(shell, "Running failed: " + e.getMessage());
			return null;
		} catch (SampleConfigSelectionException e) {
			if (!(e instanceof SampleConfigDialogCanceledException)){
				GtUiHelper.openErrorDialog(shell, "Running failed: " + e.getMessage());
			}
			return null;
		}
	}

	private List<IResource> createResourceList(){
		ISelection iSel = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		LinkedList<IResource> resources = GtUiHelper.getSelectedIResources(iSel, IResource.class);
		
		
		if (resources.isEmpty()){
			//try to get file from editor
			IFile file = getFileFromEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart());
			if (file != null){
				resources.add(file);
			}
		}
		return resources;
	}

	protected void modifyWorkbench() {

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		try {
			page.showView("org.globaltester.testmanager.views.ResultView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
		try {
			page.showView("org.eclipse.ui.views.ProblemView");
		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			page.showView("org.eclipse.ui.console.ConsoleView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}	
	}
	
	protected SampleConfig getSampleConfigFromDialog() throws SampleConfigSelectionException{
		SampleConfigSelectorDialog dialog = new SampleConfigSelectorDialog(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell());
		int result = dialog.open(); 
		if (result == Window.OK){
			return dialog.getSelectedSampleConfig();
		} else if (result == Window.CANCEL){
			throw new SampleConfigDialogCanceledException();
		}
		throw new SampleConfigSelectionException();
	}
	
	protected SampleConfig getLastUsedSampleConfig() {
		String lastUsedProjectName = PreferenceHelper.getPreferenceValue(Activator.getContext().getBundle().getSymbolicName(), Activator.PREFERENCE_ID_LAST_USED_SAMPLE_CONFIG_PROJECT);
		return SampleConfigManager.get(lastUsedProjectName);
	}
	
	/**
	 * This method tries to get a sample config for execution of the test cases. 
	 * @param event
	 * @return a {@link SampleConfig} object to be used
	 * @throws SampleConfigSelectionException when no {@link SampleConfig} could be determined.
	 */
	protected SampleConfig getSampleConfig(ExecutionEvent event) throws SampleConfigSelectionException{
		boolean selectionRequested = Boolean.parseBoolean(event.getParameter("org.globaltester.testrunner.ui.SelectSampleConfigParameter")); 
		SampleConfig lastUsed = getLastUsedSampleConfig();
		if (!selectionRequested && lastUsed != null){
			return lastUsed;
		}
		return getSampleConfigFromDialog();
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
	
	public List<IResource> getResources(){
		return resources;
		
	}

	/**
	 * @return true, iff the handler will ignore the dirty flag on the workbench
	 */
	public boolean isIgnoreDirty() {
		return ignoreDirty;
	}

	/**
	 * Sets the property for ignoring the dirty flag before execution.
	 * @param ignoreDirty
	 */
	public void setIgnoreDirty(boolean ignoreDirty) {
		this.ignoreDirty = ignoreDirty;
	}
	
}
