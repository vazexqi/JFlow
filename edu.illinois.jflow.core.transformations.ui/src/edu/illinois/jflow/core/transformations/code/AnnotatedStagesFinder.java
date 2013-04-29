package edu.illinois.jflow.core.transformations.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.ibm.wala.util.debug.Assertions;

/**
 * Locates the annotated stages in a method body.
 * 
 * The specific text that we are looking form is of the form // Begin StageX and // End StageX
 * 
 * Assumes that the stages are in sequential order starting from 1.
 * 
 * @author nchen
 * 
 */
public class AnnotatedStagesFinder {
	CompilationUnit cUnit;

	MethodDeclaration methodDeclaration;

	int methodStartOffset;

	int methodEndOffset;

	Map<Integer, AnnotatedStage> stages= new HashMap<Integer, AnnotatedStage>();

	private IDocument doc;

	public AnnotatedStagesFinder(CompilationUnit cUnit, IDocument doc, MethodDeclaration methodDeclaration) {
		this.cUnit= cUnit;
		this.doc= doc;
		this.methodDeclaration= methodDeclaration;
		this.methodStartOffset= methodDeclaration.getStartPosition();
		this.methodEndOffset= methodStartOffset + methodDeclaration.getLength();
	}

	public List<AnnotatedStage> locateStages() {
		@SuppressWarnings("unchecked")
		List<Comment> commentList= cUnit.getCommentList();
		for (Comment comment : commentList) {
			if (comment.isLineComment()) { // We are only looking at line comments
				int startOffset= comment.getStartPosition();
				int endOffset= startOffset + comment.getLength();
				if (isInsideMethod(startOffset, endOffset)) {
					try {
						String commentString= doc.get(comment.getStartPosition(), comment.getLength());
						Integer stageNumber= locateStageNameIfPossible(commentString);
						if (stageNumber != null) {
							AnnotatedStage stageAnnotation= stages.get(stageNumber);
							if (stageAnnotation == null) {
								AnnotatedStage newStageAnnotation= new AnnotatedStage(stageNumber, doc);
								stages.put(stageNumber, newStageAnnotation);
								stageAnnotation= newStageAnnotation;
							}
							stageAnnotation.addComment(comment);
						}
					} catch (BadLocationException e) {
						// This should not happen given that we get the offset from the compilation unit so the offsets should be valid
						e.printStackTrace();
					}
				}
			}
		}

		return stagesToList();
	}

	private boolean isInsideMethod(int startOffset, int endOffset) {
		return startOffset >= methodStartOffset && endOffset <= methodEndOffset;
	}

	private List<AnnotatedStage> stagesToList() {
		List<AnnotatedStage> stagesList= new ArrayList<AnnotatedStage>();
		for (int stageNumber= 1; stageNumber <= stages.size(); stageNumber++) {
			AnnotatedStage stageAnnotationPair= stages.get(stageNumber);
			Assertions.productionAssertion(stageAnnotationPair != null);
			stagesList.add(stageAnnotationPair);
		}
		return stagesList;
	}

	private Integer locateStageNameIfPossible(String input) {
		Pattern stagePattern= Pattern.compile("//(?:\\s*)\\b(?:Begin|End)(?:\\s+)Stage(\\d+)");
		Matcher matcher= stagePattern.matcher(input);
		if (matcher.find()) {
			String group= matcher.group(1);
			return Integer.parseInt(group);
		} else
			return null;
	}
}
