/**
 * This class derives from https://github.com/reprogrammer/keshmesh/ and is licensed under Illinois Open Source License.
 */
package edu.illinois.jflow.wala.pointeranalysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * Modified from AnalysisUtils.java, originally from Keshmesh. Authored by Mohsen Vakilian and Stas
 * Negara. Modified by Nicholas Chen.
 * 
 */
public class AnalysisUtils {

	private static final String EXTENSION_CLASSLOADER_NAME= "Extension";

	public static final String PRIMORDIAL_CLASSLOADER_NAME= "Primordial"; //$NON-NLS-1$

	private static final String OBJECT_GETCLASS_SIGNATURE= "java.lang.Object.getClass()Ljava/lang/Class;"; //$NON-NLS-1$

	/**
	 * All projects which the main Eclipse project depends on are in the Extension loader
	 * 
	 * See com.ibm.wala.ide.util.EclipseProjectPath for more information.
	 */
	public static boolean isLibraryClass(IClass klass) {
		return isExtension(klass.getClassLoader().getName());
	}

	public static boolean isLibraryClass(TypeReference typeReference) {
		return isExtension(typeReference.getClassLoader().getName());
	}

	private static boolean isExtension(Atom classLoaderName) {
		return classLoaderName.toString().equals(EXTENSION_CLASSLOADER_NAME);
	}

	public static boolean isJDKClass(IClass klass) {
		return isPrimordial(klass.getClassLoader().getName());
	}

	private static boolean isPrimordial(Atom classLoaderName) {
		return classLoaderName.toString().equals(PRIMORDIAL_CLASSLOADER_NAME);
	}

	public static boolean isObjectGetClass(IMethod method) {
		return isObjectGetClass(method.getSignature());
	}

	private static boolean isObjectGetClass(String methodSignature) {
		return methodSignature.equals(OBJECT_GETCLASS_SIGNATURE);
	}
}
