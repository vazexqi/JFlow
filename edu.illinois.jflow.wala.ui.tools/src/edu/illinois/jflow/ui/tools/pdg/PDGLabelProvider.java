package edu.illinois.jflow.ui.tools.pdg;

import java.util.Set;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.DataDependence;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.MethodParameter;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGNode;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.Statement;

public class PDGLabelProvider extends LabelProvider implements IEntityStyleProvider {

	private PDGView pdgView;

	public PDGLabelProvider(PDGView pdgView) {
		this.pdgView= pdgView;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Statement) {
			StringBuilder sb= new StringBuilder();
			Statement statement= (Statement)element;
			sb.append(statement.getSimplifiedRepresentation());
			return sb.toString();
		}
		if (element instanceof MethodParameter) {
			StringBuilder sb= new StringBuilder();
			sb.append("Method Parameter: ");
			sb.append(element.toString());
			return sb.toString();
		}
		if (element instanceof EntityConnectionData) {
			EntityConnectionData data= (EntityConnectionData)element;
			PDGNode source= (PDGNode)data.source;
			PDGNode dest= (PDGNode)data.dest;

			ProgramDependenceGraph pdg= pdgView.getPDG();
			Set<? extends DataDependence> labels= pdg.getEdgeLabels(source, dest);

			StringBuilder sb= new StringBuilder();
			for (DataDependence label : labels) {
				sb.append(label.getSimplifiedRepresentation());
				sb.append(" , ");
			}

			return sb.toString();
		}
		return "";
	}

	@Override
	public IFigure getTooltip(Object entity) {
		if (entity instanceof Statement) {
			Statement statement= (Statement)entity;
			return new Label(statement.toString());
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
