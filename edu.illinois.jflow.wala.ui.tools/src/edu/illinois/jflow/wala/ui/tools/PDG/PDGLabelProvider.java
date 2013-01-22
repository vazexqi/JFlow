package edu.illinois.jflow.wala.ui.tools.PDG;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.zest.core.viewers.EntityConnectionData;

import com.ibm.wala.ipa.slicer.Statement;

public class PDGLabelProvider extends LabelProvider {

	private final PDGView pdgView;

	public PDGLabelProvider(PDGView pdgView) {
		this.pdgView= pdgView;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Statement) {
			Statement statement= (Statement)element;
			StringBuilder nodeText= new StringBuilder(statement.toString());
			return nodeText.toString();
		}
		if (element instanceof EntityConnectionData) {

		}
		return "";
	}



}
