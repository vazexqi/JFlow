package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.Collection;

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * Takes a set of pointer keys and pretty prints them.
 * 
 * For each pointer key (we know that we are dealing with mostly instance and static keys), we do
 * not print the whole info but just filter out the class and the field name.
 * 
 * This is actually quite a tedious process since there is no standardized way to print these values
 * in WALA.
 * 
 * @author nchen
 * 
 */
public class PointerKeyPrettyPrinter {
	private PointerAnalysis pointerAnalysis; // Not sure if we are going to report this yet

	private Collection<PointerKey> keys;

	private StringBuilder sb= new StringBuilder();

	public PointerKeyPrettyPrinter(Collection<PointerKey> keys, PointerAnalysis pointerAnalysis) {
		this.keys= keys;
		this.pointerAnalysis= pointerAnalysis;
	}

	// Don't want to modify the core classes in WALA so we have to use poor-man's dispatch based on instanceof checks
	public String prettyPrint() {
		for (PointerKey key : keys) {
			dispatchPointerKey(key);
		}

		return sb.toString();
	}

	private void dispatchPointerKey(PointerKey key) {
		if (key instanceof InstanceFieldKey) {
			InstanceFieldKey instanceFieldKey= (InstanceFieldKey)key;
			handle(instanceFieldKey);
		} else if (key instanceof ArrayLengthKey) {
			ArrayLengthKey arrayLengthKey= (ArrayLengthKey)key;
			handle(arrayLengthKey);
		} else if (key instanceof ArrayContentsKey) {
			ArrayContentsKey arrayContentsKey= (ArrayContentsKey)key;
			handle(arrayContentsKey);
		} else if (key instanceof StaticFieldKey) {
			StaticFieldKey staticFieldKey= (StaticFieldKey)key;
			handle(staticFieldKey);
		} else {
			handle(key);
		}
	}

	void handle(InstanceFieldKey instanceFieldKey) {
		String template= "Field <%s> in object of type <%s> allocated in method <%s>%n.";
		String fieldName= null;
		String typeName= null;
		String methodName= null;

		FieldReference fieldRef= instanceFieldKey.getField().getReference();
		fieldName= fieldRef.getName().toString();
		typeName= formatTypeName(fieldRef);

		InstanceKey instanceKey= instanceFieldKey.getInstanceKey();
		if (instanceKey instanceof InstanceKeyWithNode) {
			InstanceKeyWithNode nodeKey= (InstanceKeyWithNode)instanceKey;
			CGNode node= nodeKey.getNode();
			MethodReference methodRef= node.getMethod().getReference();
			methodName= methodRef.getSignature();

		} else {
			// This should not happen since we will always have a context even if it is Everywhere
			Assertions.UNREACHABLE(String.format("Found an InstanceKey without a Node. It's type is %s%n and its value is: %s%n ", instanceKey.getClass(), instanceKey));
		}
		sb.append(String.format(template, fieldName, typeName, methodName));
	}

	void handle(ArrayLengthKey arrayLengthKey) {
		sb.append(String.format("%s%n", arrayLengthKey.toString()));
	}

	void handle(ArrayContentsKey arrayContentsKey) {
		sb.append(String.format("%s%n", arrayContentsKey.toString()));
	}

	void handle(StaticFieldKey staticFieldKey) {
		String template= "Static field <%s> in class <%s>.";
		String fieldName= null;
		String typeName= null;

		FieldReference fieldRef= staticFieldKey.getField().getReference();
		fieldName= fieldRef.getName().toString();
		typeName= formatTypeName(fieldRef);
		sb.append(String.format(template, fieldName, typeName));

	}

	// Default fall-back: this shouldn't happen in our case but it best to be safe
	private void handle(PointerKey key) {
		sb.append(String.format("%s%n", key.toString()));
	}

	private String formatTypeName(FieldReference reference) {
		TypeReference typeRef= reference.getDeclaringClass();
		if (typeRef.isPrimitiveType()) {
			return typeRef.getName().toString();
		} else if (typeRef.isArrayType()) {
			return typeRef.getName().toString();
		} else {
			return typeRef.getName().toString().substring(1).replace('/', '.');
		}
	}
}
