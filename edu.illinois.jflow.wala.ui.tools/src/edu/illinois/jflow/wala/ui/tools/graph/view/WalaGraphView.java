package edu.illinois.jflow.wala.ui.tools.graph.view;



import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import com.ibm.wala.util.graph.Graph;


public abstract class WalaGraphView extends ViewPart implements IZoomableWorkbenchPart {

	protected GraphViewer graphViewer;

	@Override
	public void createPartControl(Composite parent) {
		graphViewer= new GraphViewer(parent, SWT.BORDER);

		graphViewer.setContentProvider(getContentProvider());
		graphViewer.setLabelProvider(getLabelProvider());
		graphViewer.setConnectionStyle(getConnectionStyle());

		LayoutAlgorithm layout= setLayout();
		graphViewer.setLayoutAlgorithm(layout, true);
		graphViewer.applyLayout();
		fillToolBar();
	}


	protected abstract LabelProvider getLabelProvider();

	protected abstract IStructuredContentProvider getContentProvider();

	protected int getConnectionStyle() {
		return ZestStyles.CONNECTIONS_DIRECTED;
	}

	protected abstract void fillToolBar();

	public void updateGraph(Graph<?> graph) {
		graphViewer.setInput(graph);
		graphViewer.refresh();
	}

	@Override
	public void setFocus() {
		// Does nothing by default, subclasses can override
	}

	protected LayoutAlgorithm setLayout() {
		TreeLayoutAlgorithm layout;
		layout= new TreeLayoutAlgorithm();
		layout.setResizing(false);
		return layout;
	}

	@Override
	public AbstractZoomableViewer getZoomableViewer() {
		return graphViewer;
	}

}
