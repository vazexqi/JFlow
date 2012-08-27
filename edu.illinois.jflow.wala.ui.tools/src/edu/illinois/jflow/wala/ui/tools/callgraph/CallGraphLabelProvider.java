package edu.illinois.jflow.wala.ui.tools.callgraph;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.zest.core.viewers.EntityConnectionData;

import com.ibm.wala.ipa.callgraph.CGNode;

public class CallGraphLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof CGNode) {
			CGNode node= (CGNode)element;
			StringBuilder nodeText= new StringBuilder("Method: " + node.getMethod().toString() + "\n");
			nodeText.append("Context: " + node.getContext().toString());
			return nodeText.toString();
		}
		if (element instanceof EntityConnectionData) {

		}
		return "";
	}
}
