/**
 * This class derives
 * from {@link org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider} and is
 * licensed under the Eclipse Public License.
 */
package edu.illinois.jflow.editor.enhancements;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.projection.IProjectionPosition;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;

/**
 * Provides some nicer folding for the DataflowMessagingRunnable class used in JFlow. NOTE: A strong
 * requirement for this class is that we reuse as much as possible from
 * DefaultJavaFoldingStructureProvider *but* without changing any of its internals. This allows us
 * to ship an unmodified copy of the JDT.
 * 
 * This class can be generalized to apply to all single method anonymous inner classes, e.g., lambda
 * 
 * @author Nicholas Chen
 */
public class JFlowJavaFoldingStructureProvider extends DefaultJavaFoldingStructureProvider {

	@Override
	protected void computeFoldingStructure(IJavaElement element, FoldingStructureComputationContext ctx) {
		if (isDataflowMessagingRunnable(element)) {
			computeJFlowFoldingStructure((IType)element, ctx);

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

	private void computeJFlowFoldingStructure(IType element, FoldingStructureComputationContext ctx) {
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
				Position position= element instanceof IMember ? createJFlowClosurePosition(normalized, (IMember)element) : createCommentPosition(normalized);
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
	 * @see org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider.isInnerType(IType)
	 */
	private boolean isInnerType(IType type) {
		return type.getDeclaringType() != null;
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider.isAnonymousEnum(IType)
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

	/*
	 * @see org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider.createMemberPosition(IRegion, IMember)
	 */
	protected Position createJFlowClosurePosition(IRegion aligned, IMember member) {
		return new JFlowMemberPosition(aligned.getOffset(), aligned.getLength(), member);
	}

	/*
	 * org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider.JavaElementPosition
	 */
	private static final class JFlowMemberPosition extends Position implements IProjectionPosition {
		private IMember fMember;

		public JFlowMemberPosition(int offset, int length, IMember member) {
			super(offset, length);
			Assert.isNotNull(member);
			fMember= member;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
			int nameStart= offset;
			try {
				ISourceRange nameRange= fMember.getNameRange();
				if (nameRange != null)
					nameStart= nameRange.getOffset();

			} catch (JavaModelException e) {
				// ignore and use default
			}

			int firstLine= document.getLineOfOffset(offset);
			int captionLine= document.getLineOfOffset(nameStart);
			int lastLine= document.getLineOfOffset(offset + length);

			if (captionLine < firstLine)
				captionLine= firstLine;
			if (captionLine > lastLine)
				captionLine= lastLine;

			IRegion preName= null;
			IRegion preMethodRegion= null;
			IRegion postMethodRegion= null;

			if (firstLine < captionLine) {
				int preOffset= document.getLineOffset(firstLine);
				preName= new Region(preOffset, nameStart - preOffset);
			} else {
				preName= null;
			}

			if (captionLine < lastLine) {

				//                             !! Show this region // Prename region
				// someAnonymousClass() {      !! Show this region // CaptionLine
				//                             !! Hide this region // Premethod region
				//   void someMethod() {       !! Hide this region // Premethod region
				//    ...                      !! Show this region
				//    ...                      !! Show this region
				//   }                         !! Hide this region // Postmethod region
				//                             !! Hide this region // Postmethod region
				// }                           !! Show this region

				//TODO: Might be more robust to call the scanner so that we can account for different formatting styles

				try {
					IMethod doRunMethod; // There should only be one child
					IJavaElement possibleMethod= fMember.getChildren()[0];
					if (possibleMethod instanceof IMethod) {

						doRunMethod= (IMethod)possibleMethod;
						if (doRunMethod != null) {
							ISourceRange methodNameRange= doRunMethod.getNameRange();
							if (SourceRange.isAvailable(methodNameRange)) {
								preMethodRegion= handlePreMethodRegion(document, captionLine, methodNameRange);
							}

							ISourceRange methodRange= doRunMethod.getSourceRange();
							if (SourceRange.isAvailable(methodRange)) {
								int postMethodOffset= methodRange.getOffset() + methodRange.getLength();
								int lastLineOffset= document.getLineOffset(lastLine - 1);
								postMethodRegion= new Region(postMethodOffset, lastLineOffset - postMethodOffset);
							}
						}
					}
				} catch (JavaModelException e) {
					// If we have a JavaModelException it means that our methods are not well-formed yet. In that case, don't fold anything
					return null;
				}

			}

			List<IRegion> regions= new ArrayList<IRegion>();
			if (preName != null)
				regions.add(preName);
			if (preMethodRegion != null)
				regions.add(preMethodRegion);
			if (postMethodRegion != null)
				regions.add(postMethodRegion);

			return regions.toArray(new IRegion[regions.size()]);
		}

		private IRegion handlePreMethodRegion(IDocument document, int captionLine, ISourceRange methodNameRange) throws BadLocationException {
			IRegion preMethodRegion;
			IRegion classInfo= document.getLineInformation(captionLine + 1);

			int methodNameLine= document.getLineOfOffset(methodNameRange.getOffset());
			int methodBodyLine= methodNameLine + 1; //TODO: This can be improved, the method body need not be the immediate next line

			IRegion methodBodyInfo= document.getLineInformation(methodBodyLine);
			int methodBodyOffset= methodBodyInfo.getOffset();
			int classOffset= classInfo.getOffset();
			preMethodRegion= new Region(classOffset, methodBodyOffset - classOffset);
			return preMethodRegion;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			int nameStart= offset;
			try {
				ISourceRange nameRange= fMember.getNameRange();
				if (nameRange != null)
					nameStart= nameRange.getOffset();
			} catch (JavaModelException e) {
			}

			return nameStart - offset;
		}

	}
}
