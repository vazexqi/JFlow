package edu.illinois.jflow.wala.ui.tools.callgraph;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;

import edu.illinois.jflow.wala.ui.tools.graph.view.WalaGraphView;


public class CallGraphView extends WalaGraphView {

	public static final String CallGraphViewID= "edu.illinois.jflow.wala.ui.tools.callgraph.CallGraphView";

	@Override
	protected LabelProvider getLabelProvider() {
		return new CallGraphLabelProvider();
	}

	@Override
	protected IStructuredContentProvider getContentProvider() {
		return new CallGraphContentProvider();
	}

	@Override
	protected void fillToolBar() {
		IMenuManager menuManager= getViewSite().getActionBars().getMenuManager();
		IToolBarManager toolBarManager= getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(new GenerateCallGraph(this));
		menuManager.add(new ZoomContributionViewItem(this));
	}
}
