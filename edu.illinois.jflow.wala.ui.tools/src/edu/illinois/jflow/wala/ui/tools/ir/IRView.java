package edu.illinois.jflow.wala.ui.tools.ir;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalShiftAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import com.ibm.wala.ssa.IR;

import edu.illinois.jflow.wala.ui.tools.graph.view.WalaGraphView;

public class IRView extends WalaGraphView {

	public static final String WalaIRViewID= "edu.illinois.jflow.wala.ui.tools.ir.irview";

	private IR ir;

	@Override
	protected LabelProvider getLabelProvider() {
		return new IRLabelProvider(this);
	}

	@Override
	protected IStructuredContentProvider getContentProvider() {
		return new IRContentProvider();
	}

	@Override
	protected void fillToolBar() {
		IMenuManager menuManager= getViewSite().getActionBars().getMenuManager();
		IToolBarManager toolBarManager= getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(new GenerateIRAction(this));
		menuManager.add(new ZoomContributionViewItem(this));
	}

	@Override
	protected LayoutAlgorithm setLayout() {
		TreeLayoutAlgorithm treeLayout= new TreeLayoutAlgorithm(TreeLayoutAlgorithm.TOP_DOWN);
		treeLayout.setResizing(false);
		HorizontalShiftAlgorithm horizontalShift= new HorizontalShiftAlgorithm();
		return new CompositeLayoutAlgorithm(new LayoutAlgorithm[] { treeLayout, horizontalShift });
	}

	public void setIR(IR ir) {
		this.ir= ir;
	}

	public IR getIR() {
		return ir;
	}
}
