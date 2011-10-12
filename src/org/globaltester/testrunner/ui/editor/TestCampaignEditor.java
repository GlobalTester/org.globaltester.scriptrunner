package org.globaltester.testrunner.ui.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.globaltester.testrunner.ui.Activator;

public class TestCampaignEditor extends EditorPart {
	public TestCampaignEditor() {
	}

	public static final String ID = "org.globaltester.testrunner.ui.testcampaigneditor";
	private TestCampaignEditorInput input;
	private TreeViewer treeViewer;
	private boolean dirty = false;

	@Override
	public void doSave(IProgressMonitor monitor) {
		//TODO handle progress in monitor
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
				input = new TestCampaignEditorInput((FileEditorInput) input);
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
		parent.setLayout(new GridLayout(1, false));

		// some meta data on top of the editor
		Composite metaDataComp = new Composite(parent, SWT.NONE);
		metaDataComp.setLayout(new GridLayout(8, false));
		metaDataComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);

		Label lblMetadataHere = new Label(metaDataComp, SWT.NONE);
		lblMetadataHere.setText("MetaData here");

		// main part of the editor is occupied by tree view
		Composite treeViewerComp = new Composite(parent, SWT.NONE);
		treeViewerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 1, 1));
		treeViewerComp.setLayout(new FillLayout(SWT.HORIZONTAL));
		Tree executionStateTree = new Tree(treeViewerComp, SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL);
		executionStateTree.setHeaderVisible(true);
		treeViewer = new TreeViewer(executionStateTree);

		TreeColumn column1 = new TreeColumn(executionStateTree, SWT.LEFT);
		executionStateTree.setLinesVisible(true);
		column1.setAlignment(SWT.LEFT);
		column1.setText("Testcase/TestStep");
		column1.setWidth(250);
		TreeColumn column2 = new TreeColumn(executionStateTree, SWT.RIGHT);
		column2.setAlignment(SWT.LEFT);
		column2.setText("LastExecuted");
		column2.setWidth(120);
		TreeColumn column3 = new TreeColumn(executionStateTree, SWT.RIGHT);
		column3.setAlignment(SWT.LEFT);
		column3.setText("Result");
		column3.setWidth(120);
		TreeColumn column4 = new TreeColumn(executionStateTree, SWT.RIGHT);
		column4.setAlignment(SWT.LEFT);
		column4.setText("Comment");
		column4.setWidth(300);

		treeViewer.setContentProvider(new TestCampaignContentProvider());
		treeViewer.setLabelProvider(new TestCampaignTableLabelProvider());
		treeViewer.setInput(input.getGtTestCampaignProject());
		treeViewer.expandAll();

		// below a little button area to control execution and report generation
		Composite buttonAreaComp = new Composite(parent, SWT.NONE);
		buttonAreaComp.setLayout(new FillLayout(SWT.HORIZONTAL));
		buttonAreaComp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true,
				false, 1, 1));

		Button btnExecute = new Button(buttonAreaComp, SWT.NONE);
		btnExecute.setText("Execute");
		btnExecute.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					// execute tests
					input.getTestCampaign().executeTests();
					
					// flag the editor as dirty, so that changes can be saved
					setDirty(true);
					
					// refresh the workspace
					ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
					
				} catch (CoreException ex) {
					StatusManager.getManager().handle(ex, Activator.PLUGIN_ID);
				}
				
			}
		});

		Button btnGenerateReport = new Button(buttonAreaComp, SWT.NONE);
		btnGenerateReport.setText("Generate Report");
		btnGenerateReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO integrate report generation here
				MessageDialog
						.openWarning(Activator.getDefault().getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								"GlobalTester",
								"Report generation is not yet supported in GlobalTester3");
			}

		});

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	public void setDirty(boolean dirty) {
        this.dirty = dirty;
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

}
