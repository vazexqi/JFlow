package edu.illinois.jflow.source.utils;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * This class finds all the bindings (there should only ever be one unless something is seriously
 * wrong), for the given name. The main purpose is to serve as a bridge between the Wala analysis
 * and the JDT analysis.
 * 
 * Because the matching is only done on names, please give it the most specific starting ASTNode to
 * visit, e.g., the directly encompassing node. This not only makes the search go faster but also
 * reduces the chances that you are matching something wrongly.
 * 
 * @author nchen
 * 
 */
public class SimpleNameBindingFinder extends ASTVisitor {
	private String name;

	private Set<IBinding> bindings= new HashSet<IBinding>();

	private Class<? extends IBinding> specificType;

	public SimpleNameBindingFinder(String name) {
		this.name= name;
	}

	public SimpleNameBindingFinder(String name, Class<? extends IBinding> specificType) {
		this.name= name;
		this.specificType= specificType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
	 * 
	 * Visits all the "SimpleNames" and try to find one that matches the name that we are searching for.
	 */
	@Override
	public boolean visit(SimpleName node) {
		if (node.getIdentifier().equals(name)) {
			IBinding binding= node.resolveBinding();
			if (specificType != null) { // We are looking for a specific type of binding
				if (specificType.isInstance(binding))
					bindings.add(binding);
			} else { // If we are not looking a specific type, then just store that binding
				bindings.add(binding);
			}
		}
		return false; // No need to visit the children. This is the finest level that we need to go.
	}

	public Set<IBinding> getBindings() {
		return bindings;
	}

}
