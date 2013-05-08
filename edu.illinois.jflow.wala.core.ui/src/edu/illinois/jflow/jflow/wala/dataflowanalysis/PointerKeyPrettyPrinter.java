package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.List;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
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
	public static String prettyPrint(List<PointerKey> pointerKeys) {
		StringBuilder sb= new StringBuilder();

		for (PointerKey key : pointerKeys) {
			sb.append(dispatchPointerKey(key));
		}

		return sb.toString();
	}

	public static String prettyPrint(PointerKey pointerKey) {
		return dispatchPointerKey(pointerKey);
	}

	public static String dispatchPointerKey(PointerKey key) {
		if (key instanceof InstanceFieldKey) {
			InstanceFieldKey instanceFieldKey= (InstanceFieldKey)key;
			return handle(instanceFieldKey);
		} else if (key instanceof ArrayLengthKey) {
			ArrayLengthKey arrayLengthKey= (ArrayLengthKey)key;
			return handle(arrayLengthKey);
		} else if (key instanceof ArrayContentsKey) {
			ArrayContentsKey arrayContentsKey= (ArrayContentsKey)key;
			return handle(arrayContentsKey);
		} else if (key instanceof StaticFieldKey) {
			StaticFieldKey staticFieldKey= (StaticFieldKey)key;
			return handle(staticFieldKey);
		} else {
			return handle(key);
		}
	}

	static String handle(InstanceFieldKey instanceFieldKey) {
		StringBuilder sb= new StringBuilder();
		String template= "Field <%s> in instance[@%d] of class <%s> allocated in method <%s>.%n";
		String fieldName= null;
		int instanceID= -1;
		String typeName= null;
		String methodName= null;

		FieldReference fieldRef= instanceFieldKey.getField().getReference();
		fieldName= fieldRef.getName().toString();
		typeName= formatTypeName(fieldRef);

		InstanceKey instanceKey= instanceFieldKey.getInstanceKey();
		if (instanceKey instanceof AllocationSiteInNode) {
			AllocationSiteInNode allocSiteInNode= (AllocationSiteInNode)instanceKey;
			instanceID= allocSiteInNode.getSite().getProgramCounter();
			CGNode node= allocSiteInNode.getNode();
			MethodReference methodRef= node.getMethod().getReference();
			methodName= methodRef.getSignature();
			sb.append(String.format(template, fieldName, instanceID, typeName, methodName));
		} else {
			typeName= formatTypeName(instanceKey.getConcreteType().getReference());
			sb.append(String.format("%s.%n", typeName));
		}
		return sb.toString();
	}

	static String handle(ArrayLengthKey arrayLengthKey) {
		StringBuilder sb= new StringBuilder();
		String template= "Accessing length field of array[%d] of type <%s> allocated in method <%s>.%n";
		int instanceID= -1;
		String typeName= null;
		String methodName= null;

		InstanceKey instanceKey= arrayLengthKey.getInstanceKey();
		typeName= formatTypeName(instanceKey.getConcreteType().getReference());
		if (instanceKey instanceof AllocationSiteInNode) {
			AllocationSiteInNode allocSiteInNode= (AllocationSiteInNode)instanceKey;
			instanceID= allocSiteInNode.getSite().getProgramCounter();
			CGNode node= allocSiteInNode.getNode();
			MethodReference methodRef= node.getMethod().getReference();
			methodName= methodRef.getSignature();

		} else {
			sb.append(String.format("%s.%n", instanceKey));
		}
		sb.append(String.format(template, instanceID, typeName, methodName));
		return sb.toString();
	}

	static String handle(ArrayContentsKey arrayContentsKey) {
		StringBuilder sb= new StringBuilder();
		String template= "Content of array[@%d] of type <%s> allocated in method <%s>.%n";
		int instanceID= -1;
		String typeName= null;
		String methodName= null;

		InstanceKey instanceKey= arrayContentsKey.getInstanceKey();
		typeName= formatTypeName(instanceKey.getConcreteType().getReference());
		if (instanceKey instanceof AllocationSiteInNode) {
			AllocationSiteInNode allocSiteInNode= (AllocationSiteInNode)instanceKey;
			instanceID= allocSiteInNode.getSite().getProgramCounter();
			CGNode node= allocSiteInNode.getNode();
			MethodReference methodRef= node.getMethod().getReference();
			methodName= methodRef.getSignature();

		} else {
			sb.append(String.format("%s.%n", instanceKey));
		}
		sb.append(String.format(template, instanceID, typeName, methodName));
		return sb.toString();
	}

	static String handle(StaticFieldKey staticFieldKey) {
		StringBuilder sb= new StringBuilder();
		String template= "Static field <%s> in class <%s>.%n";
		String fieldName= null;
		String typeName= null;

		FieldReference fieldRef= staticFieldKey.getField().getReference();
		fieldName= fieldRef.getName().toString();
		typeName= formatTypeName(fieldRef);
		sb.append(String.format(template, fieldName, typeName));
		return sb.toString();
	}

	// Default fall-back: this shouldn't happen in our case but it best to be safe
	static String handle(PointerKey key) {
		StringBuilder sb= new StringBuilder();
		Assertions.UNREACHABLE("REACHED A DEFAULT POINTER-KEY");
		sb.append(String.format("%s%n", key.toString()));
		return sb.toString();
	}

	private static String formatTypeName(FieldReference reference) {
		TypeReference typeRef= reference.getDeclaringClass();
		return formatTypeName(typeRef);
	}

	private static String formatTypeName(TypeReference typeRef) {
		if (typeRef.isPrimitiveType()) {
			return typeRef.getName().toString();
		} else if (typeRef.isArrayType()) {
			return typeRef.getName().toString();
		} else {
			return typeRef.getName().toString().substring(1).replace('/', '.');
		}
	}

}
