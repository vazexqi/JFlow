package edu.illinois.jflow.source.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;

import com.ibm.wala.util.debug.Assertions;

/**
 * Acts as a facade to find all the bindings from simple names
 * 
 * @author nchen
 * 
 */
public class BindingsFinder {
	Map<String, IBinding> names2Bindings= new HashMap<String, IBinding>();

	private ASTNode[] nodes;

	private Set<String> names;

	public static Map<String, IBinding> findBindings(ASTNode[] nodes, Set<String> names) {
		BindingsFinder bfc= new BindingsFinder(nodes, names);
		bfc.findAllBindings();
		return bfc.names2Bindings;

	}

	private BindingsFinder(ASTNode[] nodes, Set<String> names) {
		this.nodes= nodes;
		this.names= names;
	}

	private void findAllBindings() {
		for (String name : names) {
			SimpleNameBindingFinder finder= new SimpleNameBindingFinder(name);
			for (ASTNode node : nodes) {
				node.accept(finder);
				List<IBinding> bindings= new ArrayList<IBinding>(finder.getBindings());
				if (bindings != null) {
					if (bindings.size() == 1) {
						names2Bindings.put(name, bindings.get(0));
						break; // Found it, no need to look through anymore
					}
					else if (bindings.size() >= 1) {
						Assertions.UNREACHABLE("Found more than one relevant binding!");
					}
				}
			}
			Assertions.productionAssertion(names2Bindings.get(name) != null, String.format("Missing binding for %s.", name));
		}

	}
}
