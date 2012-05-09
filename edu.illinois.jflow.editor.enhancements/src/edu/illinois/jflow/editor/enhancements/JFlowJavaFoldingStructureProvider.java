/**
 * This class derives
 * from {@link org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider} and is
 * licensed under the Eclipse Public License.
 */
package edu.illinois.jflow.editor.enhancements;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;

/**
 * Provides some nicer folding for the DataflowMessagingRunnable class used in JFlow.
 * 
 * @author Nicholas Chen
 */
public class JFlowJavaFoldingStructureProvider extends DefaultJavaFoldingStructureProvider {

	@Override
	protected void computeFoldingStructure(IJavaElement element, FoldingStructureComputationContext ctx) {
		if (isDataflowMessagingRunnable(element)) {
			computeJFLowFoldingStructure((IType)element, ctx);

		} else if (isNestedDataflowMessagingRunnableMethod(element)) {
			// If we have a IJavaElement.METHOD that is nested don't delegate to the superclass
			// since this might mess up the folding with overlapping regions. Just DO NOTHING
		} else {
			super.computeFoldingStructure(element, ctx);
		}
	}

	private boolean isDataflowMessagingRunnable(IJavaElement element) {
		if (element.getElementType() == IJavaElement.TYPE) {
			IType type= (IType)element;
			if (isInnerType(type) && !isAnonymousEnum(type)) {
				try {
					String superclassName= type.getSuperclassName();
					if (superclassName == null)
						return false;
					if (superclassName.matches(ExtractClosureRefactoring.CLOSURE_TYPE))
						return true;
				} catch (JavaModelException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return false;
	}

	private boolean isNestedDataflowMessagingRunnableMethod(IJavaElement element) {
		if (element.getElementType() == IJavaElement.METHOD) {
			IMethod method= (IMethod)element;
			if (isDataflowMessagingRunnable(method.getParent())) {
				if (element.getElementName().matches(ExtractClosureRefactoring.CLOSURE_METHOD)) {
					return true;
				}
			}
		}
		return false;
	}

	private void computeJFLowFoldingStructure(IType element, FoldingStructureComputationContext ctx) {
		IRegion[] regions= computeProjectionRanges((ISourceReference)element, ctx);
		if (regions.length > 0) {
			// comments
			for (int i= 0; i < regions.length - 1; i++) {
				IRegion normalized= alignRegion(regions[i], ctx);
				if (normalized != null) {
					Position position= createCommentPosition(normalized);
					if (position != null) {
						boolean commentCollapse;
						Boolean hasHeaderComment= (Boolean)callPrivateContextMethod(ctx, "hasHeaderComment");
						IType firstType= (IType)callPrivateContextMethod(ctx, "getFirstType");
						if (i == 0 && (regions.length > 2 || hasHeaderComment && element == firstType)) {
							commentCollapse= ctx.collapseHeaderComments();
						} else {
							commentCollapse= ctx.collapseJavadoc();
						}
						ctx.addProjectionRange(new JavaProjectionAnnotation(commentCollapse, element, true), position);
					}
				}
			}
			// code
			IRegion normalized= alignRegion(regions[regions.length - 1], ctx);
			if (normalized != null) {
				Position position= element instanceof IMember ? createMemberPosition(normalized, (IMember)element) : createCommentPosition(normalized);
				if (position != null)
					ctx.addProjectionRange(new JavaProjectionAnnotation(true, element, false), position);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	// Duplicated from org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider
	// because we don't want to modify the JDT class to change the visibility of the following
	// convenience methods
	////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider.isInnerType(IType)
	 */
	private boolean isInnerType(IType type) {
		return type.getDeclaringType() != null;
	}

	/*
	 * See org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider.isAnonymousEnum(IType)
	 */
	private boolean isAnonymousEnum(IType type) {
		try {
			return type.isEnum() && type.isAnonymous();
		} catch (JavaModelException x) {
			return false; // optimistically
		}
	}

	// Use reflection to access some of the private methods in FoldingStructureComputationContext
	private Object callPrivateContextMethod(FoldingStructureComputationContext ctx, String methodName) {
		try {
			Method method= FoldingStructureComputationContext.class.getDeclaredMethod(methodName, (Class<?>[])null);
			method.setAccessible(true);
			return method.invoke(ctx, (Object)null);

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

}
