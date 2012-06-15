package edu.illinois.jflow.wala.ui.tools.callgraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.graph.Graph;

public class CallGraphContentProvider implements IGraphEntityContentProvider {

	private Graph<CGNode> graph;

	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		viewer.refresh();
		graph= (Graph<CGNode>)newInput;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Graph) {
			Graph<?> graph= (Graph<?>)inputElement;
			List<Object> nodes= new ArrayList<Object>();
			for (Object object : graph) {
				nodes.add(object);
			}
			return nodes.toArray();
		}
		return null;
	}

	@Override
	public Object[] getConnectedTo(Object entity) {
		Iterator<CGNode> succNodes= graph.getSuccNodes((CGNode)entity);
		List<Object> nodes= new ArrayList<Object>();
		while (succNodes.hasNext()) {
			nodes.add(succNodes.next());
		}
		return nodes.toArray();
	}

	@Override
	public void dispose() {
	}

}
