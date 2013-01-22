package edu.illinois.jflow.wala.ui.tools.PDG;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalShiftAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import edu.illinois.jflow.wala.ui.tools.graph.view.WalaGraphView;

public class PDGView extends WalaGraphView {

	@Override
	protected LabelProvider getLabelProvider() {
		return new PDGLabelProvider(this);
	}

	@Override
	protected IStructuredContentProvider getContentProvider() {
		return new PDGContentProvider();
	}

	@Override
	protected LayoutAlgorithm setLayout() {
		TreeLayoutAlgorithm treeLayout= new TreeLayoutAlgorithm(TreeLayoutAlgorithm.TOP_DOWN);
		treeLayout.setResizing(false);
		HorizontalShiftAlgorithm horizontalShift= new HorizontalShiftAlgorithm();
		return new CompositeLayoutAlgorithm(new LayoutAlgorithm[] { treeLayout, horizontalShift });
	}

	@Override
	protected void fillToolBar() {
		IMenuManager menuManager= getViewSite().getActionBars().getMenuManager();
		IToolBarManager toolBarManager= getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(new GeneratePDG(this));
		menuManager.add(new ZoomContributionViewItem(this));
	}

}
