package edu.illinois.jflow.wala.ui.tools.pdg;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;

public class PDGLabelProvider extends LabelProvider implements IEntityStyleProvider {

	private final PDGView pdgView;

	public PDGLabelProvider(PDGView pdgView) {
		this.pdgView= pdgView;
	}

	@Override
	public String getText(Object element) {
		// Do it in this order, prefer StatementWithInstruction when possible
		if (element instanceof StatementWithInstructionIndex) {
			StatementWithInstructionIndex statement= (StatementWithInstructionIndex)element;
			StringBuilder sb= new StringBuilder();
			sb.append(statement.getKind().toString());
			sb.append("\n");
			sb.append("[" + statement.getInstructionIndex() + "] ");
			sb.append(statement.getInstruction().toString());
			return sb.toString();
		} else if (element instanceof Statement) {
			Statement statement= (Statement)element;
			CGNode node= statement.getNode();

			StringBuilder sb= new StringBuilder();
			sb.append(statement.getKind().toString());
			sb.append("\n");
			sb.append(node.getMethod().toString());
			return sb.toString();
		}
		if (element instanceof EntityConnectionData) {

		}
		return "";
	}

	@Override
	public IFigure getTooltip(Object entity) {
		if (entity instanceof Statement) {
			Statement statement= (Statement)entity;
			StringBuilder sb= new StringBuilder(statement.toString());
			return new Label(sb.toString());
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// Configurations that we don't care about but have to implement as part of IEntityStyleProvider
	////////////////////////////////////////////////////////////////////////////////////////////////

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
