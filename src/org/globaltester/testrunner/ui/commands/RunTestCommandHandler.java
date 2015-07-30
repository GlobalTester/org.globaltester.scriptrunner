package org.globaltester.testrunner.ui.commands;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.globaltester.cardconfiguration.CardConfig;
import org.globaltester.cardconfiguration.CardConfigManager;
import org.globaltester.cardconfiguration.GtCardConfigNature;
import org.globaltester.cardconfiguration.ui.CardConfigSelectorDialog;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.GtTestCampaignNature;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor;
import org.globaltester.testrunner.ui.editor.TestCampaignEditorInput;
import org.globaltester.testspecification.ui.editors.TestSpecEditor;

public class RunTestCommandHandler extends AbstractHandler {

	private GtTestCampaignProject campaignProject = null;
	private CardConfig cardConfig = null;
	protected Shell shell = null;

	/**
	 * If in debug mode, this method starts the Rhino debugger launch. For 
	 * RunTestCommandHandler the implementation is empty. Derived classes 
	 * can override this method.
	 * @param event needed to retrieve information on the currently selected 
	 * 			resource
	 * @throws RuntimeException only in derived classes
	 */
	protected void startRhinoDebugLaunch(ExecutionEvent event)  throws RuntimeException {
		// nothing to do in this class
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}
		
		// get TestCampaign from user selection
		campaignProject = null;

		shell = HandlerUtil.getActiveWorkbenchWindow(event).getShell();
		IWorkbenchPart activePart = Activator.getDefault().getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();

		if (activePart instanceof EditorPart) {
			campaignProject = getCampaignProjectFromEditor(activePart);

			if (campaignProject == null) {
				// no campaignProject available, inform user
				GtUiHelper.openErrorDialog(shell,
						"No TestCampaignProject could be associated with active editor. " +
						"Please select either an existing TestCampaign for execution or " + 
						"valid input to create a new one.");
				return null;
			}
		} else {
			ISelection iSel = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getSelectionService()
					.getSelection();

			campaignProject = getCampaignProjectFromSelection(iSel, shell);
		}

		if (campaignProject == null) {
			// no campaignProject available, user is already informed
			return null;
		}

		// try to get CardConfig
		cardConfig = null;
		String selectCardConfigParam = event
				.getParameter("org.globaltester.testrunner.ui.SelectCardConfigParameter");
		boolean forceSelection = (selectCardConfigParam != null)
				&& selectCardConfigParam.trim().toLowerCase().equals("true");
		if (!forceSelection) {
			// try to get CardConfig from last CampaignExecution
			cardConfig = getLastCardConfigFromTestCampaignProject(campaignProject);

			// try to get CardConfig from Selection if none was defined in
			// TestCampaign
			if (cardConfig == null) {
				cardConfig = getFirstCardConfigFromSelection();
			}
		}

		// ask user for CardConfig if none was selected
		if (cardConfig == null) {
			CardConfigSelectorDialog dialog = new CardConfigSelectorDialog(
					HandlerUtil.getActiveWorkbenchWindow(event).getShell());
			if (dialog.open() != Window.OK) {
				return null;
			}
			cardConfig = dialog.getSelectedCardConfig();
		}

		/*
		 * Tries to start the Rhino JavaScript debugger launch in an own thread.
		 * Concurrently the Rhino debugger thread is started by executeTests()
		 * below. This Rhino debugger thread must be started before the launch
		 * thread and the debugger launch thread has to wait for it, since these
		 * two Rhino threads communicate with each other.
		 */
		try {
			startRhinoDebugLaunch(event);
//			if (true) // only used for testing the XML converter
//				return null; //TODO delete this!!
		}
		catch (Exception exc) {
			// special exception handling has already been done in startRhinoDebugLaunch()
			return null; // TODO amay: return null or Status.ERROR?
		}


		// execute the TestCampaign

		Job job = new Job("Test execution") {

			protected IStatus run(IProgressMonitor monitor) {
				// execute tests
				try {
					campaignProject.getTestCampaign().executeTests(cardConfig,
							monitor, isDebugMode());
				} catch (CoreException e) {
					GtErrorLogger.log(Activator.PLUGIN_ID, e);
				}

				// refresh the workspace
				try {
					ResourcesPlugin.getWorkspace().getRoot()
							.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					// log Exception to eclipse log
					GtErrorLogger.log(Activator.PLUGIN_ID, e);

					// users most probably will ignore this behavior and refresh
					// workspace manually, so do not open annoying dialog
				}

				// open the new TestCampaign in the Test Campaign Editor
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {
								try {
									GtUiHelper.openInEditor(campaignProject
											.getTestCampaignIFile());
								} catch (CoreException e) {
									// log Exception to eclipse log
									GtErrorLogger.log(Activator.PLUGIN_ID, e);

									// users most probably will ignore this
									// behavior and open editor manually, so do
									// not open annoying dialog
								}
							}
						});

				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();

		return null;
	}

	private GtTestCampaignProject getCampaignProjectFromSelection(
			ISelection iSel, Shell shell) {

		GtTestCampaignProject tmpCampaignProject = null;

		// if only one TestCampaign is selected run this
		LinkedList<IFile> selectedIFiles = GtUiHelper.getSelectedIResources(
				iSel, IFile.class);
		if (!selectedIFiles.isEmpty()) {
			// add the selected resources to the list of executables
			Iterator<IFile> execFilesIter = selectedIFiles.iterator();
			while (execFilesIter.hasNext()) {
				IProject curProject = execFilesIter.next().getProject();

				try {
					if (curProject.hasNature(GtTestCampaignNature.NATURE_ID)) {
						if (tmpCampaignProject == null) {
							// found first TestCampaignProject in selection
							tmpCampaignProject = GtTestCampaignProject
									.getProjectForResource(curProject);
						} else {
							GtUiHelper.openErrorDialog(shell,
									"Selection contains files from more than one " + 
									"TestCampaign. Please select only one TestCampaign to execute.");
							return null;
						}
					}
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		if (tmpCampaignProject != null) {
			return tmpCampaignProject;
		}

		// try to create a TestCampaignProject from current selection
		try {
			return CreateTestCampaignCommandHandler.createTestCampaignProject(
					iSel, shell);
		} catch (CoreException e) {
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
		}
		return null;
	}

	private GtTestCampaignProject getCampaignProjectFromEditor(
			IWorkbenchPart activePart) {
		if (activePart instanceof TestCampaignEditor) {
			TestCampaignEditorInput editorInput = (TestCampaignEditorInput) 
					((TestCampaignEditor) activePart).getEditorInput();
			return editorInput.getGtTestCampaignProject();
		} else if (activePart instanceof TestSpecEditor) {
			FileEditorInput editorInput = (FileEditorInput) ((TestSpecEditor) activePart)
					.getEditorInput();
			IFile file = editorInput.getFile();

			// try to create a TestCampaignProject from current selection
			try {
				return CreateTestCampaignCommandHandler
						.createTestCampaignProject(file, shell);
			} catch (CoreException e) {
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
		}
		return null;
	}

	private CardConfig getFirstCardConfigFromSelection() {
		ISelection iSel = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getSelectionService().getSelection();

		LinkedList<IResource> iResources = GtUiHelper.getSelectedIResources(
				iSel, IResource.class);
		for (IResource iFile : iResources) {
			IProject iProject = iFile.getProject();
			try {
				if (iProject.hasNature(GtCardConfigNature.NATURE_ID)) {
					return CardConfigManager.get(iProject.getName());
				}
			} catch (CoreException e) {
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
		}
		return null;
	}

	private CardConfig getLastCardConfigFromTestCampaignProject(
			GtTestCampaignProject parentCampaingProject) {
		TestCampaignExecution currentExecution = parentCampaingProject
				.getTestCampaign().getCurrentExecution();
		if (currentExecution != null) {
			// TODO amay: test added because of NullPointerException on empty
			// cardConfig;
			// what else should be done? Should there be an error message?
			if (currentExecution.getCardConfig() != null) {
				String cardConfigName = currentExecution.getCardConfig().getName();
				if (CardConfigManager.isAvailableAsProject(cardConfigName)) {
					return CardConfigManager.get(cardConfigName);
				}
			}
		}
		return null;
	}

	/**
	 * Indicates if JavaScript debugging is activated or not.
	 * 
	 * @return false since JavaScript debugging is deactivated for this handler
	 */
	public boolean isDebugMode() {
		return false;
	}

}
