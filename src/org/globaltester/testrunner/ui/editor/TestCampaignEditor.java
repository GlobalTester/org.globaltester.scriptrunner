package org.globaltester.testrunner.ui.editor;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditor;
import org.globaltester.cardconfiguration.CardConfig;
import org.globaltester.cardconfiguration.ui.CardConfigEditorWidget;
import org.globaltester.cardconfiguration.ui.CardConfigSelector;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.logging.logger.GTLogger;
import org.globaltester.testrunner.report.ReportPdfGenerator;
import org.globaltester.testrunner.report.TestReport;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.ActionStepExecution;
import org.globaltester.testrunner.testframework.FileTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.Result;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.UiImages;

public class TestCampaignEditor extends EditorPart implements SelectionListener, IResourceChangeListener {
	public TestCampaignEditor() {
	}
	
	public static final String ID = "org.globaltester.testrunner.ui.testcampaigneditor";
	private TestCampaignEditorInput input;
	private CardConfigEditorWidget cardConfigViewer;
	private TreeViewer treeViewer;
	private boolean dirty = false;
	private Text txtSpecName;
	private Text txtSpecVersion;
	private CardConfigSelector cardConfigSelector;

	// some actions defined for this view
	private Action actionShowTestCase;
	private Action actionShowLog;
	private Action doubleClickAction;
	private Button btnOldest;
	private Button btnStepBack;
	private Combo cmbExecutionSelector;
	private Button btnStepForward;
	private Button btnNewest;
	

	@Override
	public void doSave(IProgressMonitor monitor) {
		//TODO handle progress in monitor
		
		//save selectedCardConfiguration
		cardConfigViewer.doSave();
		
		//flush all changed values to the input
		input.getTestCampaign().setSpecName(txtSpecName.getText());
		input.getTestCampaign().setSpecVersion(txtSpecVersion.getText());
		
		try {
			input.getGtTestCampaignProject().doSave();
		} catch (CoreException e) {
			StatusManager.getManager().handle(e, Activator.PLUGIN_ID);
		}
		setDirty(false);
	}

	@Override
	public void doSaveAs() {
		// SaveAs is not allowed, see isSaveAsAllowed()
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
	throws PartInitException {
		if ((input instanceof FileEditorInput)) {
			try {
				input = new TestCampaignEditorInput(((FileEditorInput) input).getFile());
			} catch (CoreException e) {
				throw new RuntimeException(
				"Wrong Input - No TestCampaignEditorInput can be created from selected resource");
			}
		}
		if (!(input instanceof TestCampaignEditorInput)) {
			throw new RuntimeException("Wrong input");
		}

		this.input = (TestCampaignEditorInput) input;
		setSite(site);
		setInput(input);
		setDirty(false);
		setPartName(this.input.getName());
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// savAs is not allowed as the editor reflects state of the complete
		// project instead of only the file
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
	    scrolledComposite.setExpandVertical(true);
	    
		Composite scrolledContent = new Composite(scrolledComposite, SWT.NONE);
		scrolledContent.setLayout(new GridLayout(1, false));
		scrolledComposite.setContent(scrolledContent);

		// some meta data on top of the editor
		Composite metaDataComp = new Composite(scrolledContent, SWT.NONE);
		metaDataComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		metaDataComp.setLayout(new GridLayout(2, false));

		Label lblSpecName = new Label(metaDataComp, SWT.NONE);
		lblSpecName.setText("Specification name:");

		txtSpecName = new Text(metaDataComp, SWT.BORDER);
		txtSpecName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtSpecName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
			}
		});
		txtSpecName.setText(input.getTestCampaign().getSpecName());

		Label lblSpecificationVersion = new Label(metaDataComp, SWT.NONE);
		lblSpecificationVersion.setText("Specification version:");

		txtSpecVersion = new Text(metaDataComp, SWT.BORDER);
		txtSpecVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1,
				1));
		txtSpecVersion.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
			}
		});
		txtSpecVersion.setText(input.getTestCampaign().getSpecVersion());
		
		Group grpExecutionresults = new Group(scrolledContent, SWT.NONE);
		grpExecutionresults.setLayout(new GridLayout(1, false));
		grpExecutionresults.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		grpExecutionresults.setText("Execution results");
		
		// history
		Composite historyComp = new Composite(grpExecutionresults, SWT.NONE);
		historyComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		historyComp.setLayout(new GridLayout(5, false));
		btnOldest = new Button(historyComp, SWT.NONE);
		btnOldest.setText("|<<");
		btnStepBack = new Button(historyComp, SWT.NONE);
		btnStepBack.setText("<");
		cmbExecutionSelector = new Combo(historyComp, SWT.NONE);
		cmbExecutionSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cmbExecutionSelector.addSelectionListener(this);
		btnStepForward = new Button(historyComp, SWT.NONE);
		btnStepForward.setText(">");
		btnStepForward.addSelectionListener(this);
		
		btnStepForward.setEnabled(false);
		btnStepBack.addSelectionListener(this);
		btnNewest = new Button(historyComp, SWT.NONE);
		btnNewest.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnNewest.setText(">>|");
		btnNewest.addSelectionListener(this);		
		btnNewest.setEnabled(false);
		
		//selection and Editor for CardConfiguration
		Composite cardConfigComp = new Composite(grpExecutionresults, SWT.NONE);
		cardConfigComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		cardConfigComp.setLayout(new GridLayout(1, false));
		cardConfigViewer = new CardConfigEditorWidget(cardConfigComp);
		cardConfigViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		cardConfigViewer.setEditable(false);
		
		Composite execStateTreeComp = new Composite(grpExecutionresults, SWT.NONE);
		execStateTreeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		Tree executionStateTree = new Tree(execStateTreeComp, SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL);
		executionStateTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
//		executionStateTree.setSize(811, 45);
		executionStateTree.setHeaderVisible(true);
		treeViewer = new AllColumnsEditableTreeViewer(executionStateTree);
		
		TreeColumn columnName = new TreeColumn(executionStateTree, SWT.LEFT);
		executionStateTree.setLinesVisible(true);
		columnName.setAlignment(SWT.LEFT);
		columnName.setText("Test case");
		TreeColumn columnLastExec = new TreeColumn(executionStateTree,
				SWT.RIGHT);
		columnLastExec.setAlignment(SWT.LEFT);
		columnLastExec.setText("Last executed");
		TreeColumn columnStatus = new TreeColumn(executionStateTree, SWT.RIGHT);
		columnStatus.setAlignment(SWT.LEFT);
		columnStatus.setText("Status");
		TreeColumn columnComment = new TreeColumn(executionStateTree, SWT.RIGHT);
		columnComment.setAlignment(SWT.LEFT);
		columnComment.setText("Comment");
		
		//make comment column editable
		TreeViewerColumn viewerColumnComment = new TreeViewerColumn(treeViewer, columnComment);
		viewerColumnComment.setEditingSupport(new EditingSupport(treeViewer) {
			
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof AbstractTestExecution && value instanceof String){
					Result result = ((AbstractTestExecution) element).getResult();
					if (!result.getComment().equals(value)){
						result.setComment((String)value);
						treeViewer.refresh();
						setDirty(true);
					}
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof AbstractTestExecution)
					return ((AbstractTestExecution) element).getResult().getComment();
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				if (element instanceof AbstractTestExecution){
					return new SilentTextCellEditor(treeViewer.getTree());
				}
				return null;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		//set column widths
		TreeColumnLayout execStateTreeLayout = new TreeColumnLayout();
		execStateTreeComp.setLayout( execStateTreeLayout );
		execStateTreeLayout.setColumnData( columnName, new ColumnWeightData( 50 ) );
		execStateTreeLayout.setColumnData( columnLastExec, new ColumnPixelData( 120 ) );
		execStateTreeLayout.setColumnData( columnStatus, new ColumnPixelData( 100 ) );
		execStateTreeLayout.setColumnData( columnComment, new ColumnWeightData( 100 ) );

		treeViewer.setContentProvider(new TestCampaignContentProvider());
		treeViewer.setLabelProvider(new TestCampaignTableLabelProvider());
		treeViewer.setInput(input.getCurrentTestCampaignExecution());
		treeViewer.expandAll();

		makeActions();
		hookContextMenu();
		hookDoubleClickAction();

		// below a little button area to report generation and maybe later other
		// tasks
		Composite buttonAreaComp = new Composite(grpExecutionresults, SWT.NONE);
		buttonAreaComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false, 1, 1));
		buttonAreaComp.setLayout(new FillLayout(SWT.HORIZONTAL));

		Button btnGenerateReport = new Button(buttonAreaComp, SWT.NONE);
		btnGenerateReport.setText("Generate Report");
		btnGenerateReport.setImage(UiImages.RESULT_ICON.getImage());
		btnGenerateReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// ask for report location
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Please select location to store the report files");
				dialog.setFilterPath(null); // do not filter at all
				String baseDirName = dialog.open();

				if (baseDirName != null) {
					// create report
					TestReport report = new TestReport(input
							.getCurrentlyDisplayedTestCampaignExecution(),
							baseDirName);

					try {
						// TODO output XML-Report here, if no pdf is desired

						// output pdf report
						ReportPdfGenerator.writePdfReport(report);
					} catch (IOException ex) {
						IStatus status = new Status(Status.ERROR,
								Activator.PLUGIN_ID,
								"PDF report could not be created", ex);
						StatusManager.getManager().handle(status,
								StatusManager.SHOW);
					}

					// TODO copy relevant logfiles
				}
			}

		});

		// Group Execution control
		Group grpExecutionControl = new Group(scrolledContent, SWT.NONE);
		grpExecutionControl.setText("Execution control");
		grpExecutionControl.setLayout(new GridLayout(2, false));
		grpExecutionControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		cardConfigSelector = new CardConfigSelector(grpExecutionControl, CardConfigSelector.ALL_BUTTONS);
		cardConfigSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Button btnExecute = new Button(grpExecutionControl, SWT.NONE);
		btnExecute.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		btnExecute.setSize(52, 25);
		btnExecute.setText("Execute");
		btnExecute.setImage(UiImages.EXECUTE_ICON.getImage());
		btnExecute.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Job job = new Job("Test execution") {

					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Execution started...", 10);
						// execute tests
						try {
							CardConfig cardConfig = cardConfigSelector
									.getSelectedConfig();
							input.getTestCampaign().executeTests(cardConfig);
						} catch (CoreException e) {
							StatusManager.getManager().handle(e,
									Activator.PLUGIN_ID);
						}
						monitor.done();
						return Status.OK_STATUS;
					}
				};
				job.setUser(true);
				job.schedule();

			}
		});
		
		updateEditor();
		
		scrolledComposite.setMinSize(scrolledContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrolledContent.layout();

		//unset dirty flag as input is just loaded from file
		setDirty(false);

	}


	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TestCampaignEditor.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(actionShowTestCase);
		manager.add(actionShowLog);
		
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void openTestCase() {
		ISelection selection = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) selection)
		.getFirstElement();
		FileTestExecution fte = null;
		
		if(obj instanceof TestCampaign){
			MessageBox dialog = new MessageBox(getShell(), SWT.APPLICATION_MODAL);
			dialog.setMessage("Open TestCase is not available for TestCampaigns");
			dialog.open();
		} else if (obj != null) {
			if (obj instanceof FileTestExecution) {
				fte = (FileTestExecution) obj;
			} else if (obj instanceof ActionStepExecution) {
				IExecution ie = ((ActionStepExecution) obj).getParent();
				if (ie instanceof FileTestExecution) {
					fte = (FileTestExecution) ie;
				}
			}
			if (fte != null) {
				IFile file = fte.getSpecFile();
				showFile(file, 0);
			}
		}
	}

	private void openLogFile() {
		ISelection selection = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj instanceof IExecution) {
			String logFileName = ((IExecution) obj).getLogFileName();
			int logFileLine = ((IExecution) obj).getLogFileLine();
			openFileOrShowErrorMessage(logFileName, logFileLine);
		} else {
			GtUiHelper.openErrorDialog(getShell(),
					"Selected element is not an IExecution");
		}
	}

	/**
	 * Define actions of this view
	 * 
	 */
	private void makeActions() {

		// show test case:
		actionShowTestCase = new Action() {
			public void run(){
				openTestCase();
			}
		};

		actionShowTestCase.setText("Show test case");
		actionShowTestCase.setToolTipText("Show test case");
		actionShowTestCase.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_PASTE));

		// show log file:
		actionShowLog = new Action() {
			public void run() {
				openLogFile();
			}
		};

		actionShowLog.setText("Show log file");
		actionShowLog.setToolTipText("Show log file");
		actionShowLog.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_PASTE));

		// double click -> show test case
		doubleClickAction = new Action() {

			public void run(){
				int customizedDoubleClick = Platform.getPreferencesService().getInt(org.globaltester.testrunner.Activator.PLUGIN_ID,
						org.globaltester.testrunner.preferences.PreferenceConstants.P_DOUBLECLICKRESULTVIEW, 0, null);
				if(customizedDoubleClick == 0) {
					openTestCase();
				}
				else{
					openLogFile();
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
				
			}
		});
	}


	/**
	 * Show files from local workspace in and editor and select given line
	 * 
	 * @param file			the IFile to be opened
	 * @param line			line to be highlighted
	 */
	private void showFile(IFile file, int line) {

		IEditorPart editor;
		ITextEditor textEditor = null;
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
		IWorkbenchPage page = window.getActivePage();

		try {
			if (file != null && file.exists()) {
				editor = IDE.openEditor(page, file, true);
				textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
			} else {
				GtUiHelper.openErrorDialog(getShell(),
						"File does not exist, thus can not be displayed.");
				return;
			}
		} catch (PartInitException ex) {
			GTLogger.getInstance().error(ex);
		}

		if ((line > 0) && (textEditor != null)) {
			try {
				line--; // document starts with 0
				IDocument document = textEditor.getDocumentProvider()
				.getDocument(textEditor.getEditorInput());

				textEditor.selectAndReveal(document.getLineOffset(line),
						document.getLineLength(line));

			} catch (BadLocationException e) {
				// invalid text position -> do nothing
			}
		}
	}

	/**
	 * Show files from local workspace in an editor and select given line
	 * 
	 * @param fileName				name of file to be opened
	 * @param line					line to be highlighted
	 */
	private void openFileOrShowErrorMessage(String fileName, int line) {

		if ((fileName == null) || (fileName == "")) {
			GtUiHelper.openErrorDialog(getShell(),
					"No file name given, thus file can not be displayed.");
			return;
		}
		
		IPath path = new Path(fileName);
		// file exists in local workspace
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
		.getFileForLocation(path);

		showFile(file, line);
	}


	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	private void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	private Shell getShell() {
		return treeViewer.getControl().getShell();
	}

	/**
	 * Updates the editor with the current displayed TestCampaignExecution using the display thread.
	 */
	private void updateEditor() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				TestCampaignExecution toDisplay = input.getCurrentlyDisplayedTestCampaignExecution();
				if (toDisplay != null) {
					cardConfigViewer.setInput(toDisplay.getCardConfig());
					treeViewer.setInput(toDisplay);
					treeViewer.expandAll();
				}
				// set buttons according to displayed TestCampaignExecution
				btnOldest.setEnabled(input.isStepBackwardsPossible());
				btnStepBack.setEnabled(input.isStepBackwardsPossible());
				btnStepForward.setEnabled(input.isStepForwardsPossible());
				btnNewest.setEnabled(input.isStepForwardsPossible());
				
				//set input for Combo and select current displayed
				cmbExecutionSelector.setItems(input.getArrayOfTestCampaignExecutions());
				cmbExecutionSelector.select(input.getIndexOfCurrentlyDisplayedTestCampaignExecution());
			}
		});		
		
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == btnNewest){
			input.stepToNewest();
		} else if (e.getSource() == btnOldest){
			input.stepToOldest();
		} else if (e.getSource() == btnStepBack){
			input.stepBackward();
		} else if (e.getSource() == btnStepForward){
			input.stepForward();
		} else if (e.getSource() == cmbExecutionSelector){
			input.stepToIndex(cmbExecutionSelector.getSelectionIndex());
		} 
		updateEditor();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta rootDelta = event.getDelta();
		if (rootDelta != null) {
			TestCampaignExecution execution = input
					.getCurrentTestCampaignExecution();
			if (execution != null) {
				// find delta for the current TestCampaignExecution
				IResourceDelta campaignExecutionDelta = rootDelta
						.findMember(input.getCurrentTestCampaignExecution()
								.getIFile().getFullPath());
				// update if ressource was a TestCampaignExecution
				if (campaignExecutionDelta != null) {
					input.stepToNewest();
					updateEditor();
				}
			}
		}
	}
}
