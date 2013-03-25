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

	private ASTNode node;

	private Set<String> names;

	public static Map<String, IBinding> findBindings(ASTNode node, Set<String> names) {
		BindingsFinder bfc= new BindingsFinder(node, names);
		bfc.findAllBindings();
		return bfc.names2Bindings;

	}

	private BindingsFinder(ASTNode node, Set<String> names) {
		this.node= node;
		this.names= names;
	}

	private void findAllBindings() {
		for (String name : names) {
			SimpleNameBindingFinder finder= new SimpleNameBindingFinder(name);
			node.accept(finder);

			List<IBinding> bindings= new ArrayList<IBinding>(finder.getBindings());

			// Sanity check to ensure that nothing went wrong
			Assertions.productionAssertion(bindings.size() == 1, "Cannot resolve the binding from JDT. Investigate!");

			names2Bindings.put(name, bindings.get(0));
		}

	}
}
