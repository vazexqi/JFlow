package edu.illinois.jflow.ui.tools.pdg;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalShiftAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;
import edu.illinois.jflow.wala.ui.tools.graph.view.WalaGraphView;

public class PDGView extends WalaGraphView {

	public static final String WalaIRViewID= "edu.illinois.jflow.ui.tools.pdg.pdgview";

	private IDocument document;

	private ProgramDependenceGraph pdg;

	@Override
	protected LabelProvider getLabelProvider() {
		return new PDGLabelProvider(this);
	}

	@Override
	protected IStructuredContentProvider getContentProvider() {
		return new PDGContentProvider();
	}

	@Override
	protected void fillToolBar() {
		IMenuManager menuManager= getViewSite().getActionBars().getMenuManager();
		IToolBarManager toolBarManager= getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(new ViewPDGAction(this));
		menuManager.add(new ZoomContributionViewItem(this));
	}

	@Override
	protected LayoutAlgorithm setLayout() {
		return new TreeLayoutAlgorithm(TreeLayoutAlgorithm.LEFT_RIGHT);
	}

	public void setDocument(IDocument document) {
		this.document= document;
	}

	public IDocument getDocument() {
		return document;
	}

	public void setPDG(ProgramDependenceGraph pdg) {
		this.pdg= pdg;
	}

	public ProgramDependenceGraph getPDG() {
		return pdg;
	}
}
