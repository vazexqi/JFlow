package edu.illinois.jflow.wala.ui.tools.callgraph;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;

import com.ibm.wala.ipa.callgraph.CGNode;

public class CallGraphLabelProvider extends LabelProvider implements IEntityStyleProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof CGNode) {
			CGNode node= (CGNode)element;
			StringBuilder sb= new StringBuilder("Method: " + node.getMethod().toString() + "\n");
			return sb.toString();
		}
		if (element instanceof EntityConnectionData) {

		}
		return "";
	}

	@Override
	public IFigure getTooltip(Object entity) {
		if (entity instanceof CGNode) {
			CGNode node= (CGNode)entity;
			StringBuilder sb= new StringBuilder();
			sb.append("Context: " + node.getContext().toString());
			return new Label(sb.toString());
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// Configurations that we don't care about but have to implement as part of IEntityStyleProvider
	////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void dispose() {

	}

	@Override
	public Color getNodeHighlightColor(Object entity) {
		return null;
	}

	@Override
	public Color getBorderColor(Object entity) {
		return null;
	}

	@Override
	public Color getBorderHighlightColor(Object entity) {
		return null;
	}

	@Override
	public int getBorderWidth(Object entity) {
		return 0;
	}

	@Override
	public Color getBackgroundColour(Object entity) {
		return null;
	}

	@Override
	public Color getForegroundColour(Object entity) {
		return null;
	}

	@Override
	public boolean fisheyeNode(Object entity) {
		return false;
	}
}
