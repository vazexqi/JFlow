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
public class StagesLocator {
	CompilationUnit cUnit;

	MethodDeclaration methodDeclaration;

	int methodStartOffset;

	int methodEndOffset;

	Map<Integer, StageAnnotationPair> stages= new HashMap<Integer, StageAnnotationPair>();

	public StagesLocator(CompilationUnit cUnit, MethodDeclaration methodDeclaration) {
		this.cUnit= cUnit;
		this.methodDeclaration= methodDeclaration;
		this.methodStartOffset= methodDeclaration.getStartPosition();
		this.methodEndOffset= methodStartOffset + methodDeclaration.getLength();
	}

	public List<StageAnnotationPair> locateStages() {
		@SuppressWarnings("unchecked")
		List<Comment> commentList= cUnit.getCommentList();
		for (Comment comment : commentList) {
			if (comment.isLineComment()) { // We are only looking at line comments
				int startOffset= comment.getStartPosition();
				int endOffset= startOffset + comment.getLength();
				if (startOffset >= methodStartOffset && endOffset <= methodEndOffset) {
					String stageName= locateStageNameIfPossible(comment.toString());
					StageAnnotationPair stageAnnotation= stages.get(stageName);
					if (stageAnnotation == null) {
						StageAnnotationPair newStageAnnotation= new StageAnnotationPair(stageName);
						stages.put(Integer.parseInt(stageName), newStageAnnotation);
						stageAnnotation= newStageAnnotation;
					}
					stageAnnotation.addComment(comment);
				}
			}
		}

		return stagesToList();
	}

	private List<StageAnnotationPair> stagesToList() {
		List<StageAnnotationPair> stagesList= new ArrayList<StageAnnotationPair>();
		for (int stageNumber= 0; stageNumber < stages.size(); stageNumber++) {
			StageAnnotationPair stageAnnotationPair= stages.get(stageNumber);
			Assertions.productionAssertion(stageAnnotationPair != null);
			stagesList.add(stageAnnotationPair);
		}
		return stagesList;
	}

	private String locateStageNameIfPossible(String input) {
		Pattern stagePattern= Pattern.compile("//(?:\\s*) \\b(?:Begin|End)\\b Stage(\\d+)");
		Matcher matcher= stagePattern.matcher(input);
		if (matcher.find()) {
			String group= matcher.group(1);
			return group;
		} else
			return null;
	}

	public static class StageAnnotationPair {
		String stageName;

		Comment start;

		Comment end;

		public StageAnnotationPair(String stageName) {
			this.stageName= stageName;
		}

		/*
		 * Assumes first comment to be added is the start comment 
		 */
		void addComment(Comment comment) {
			if (start == null) {
				start= comment;
			}
			else if (end == null) {
				end= comment;
			} else {
				throw new IllegalStateException("Already have begin and end comments");
			}
		}

		/*
		 * Returns the offset in the document, not including the start comments
		 */
		int getStageStart() {
			return start.getStartPosition() + start.getLength();
		}

		/*
		 * Returns the offset in the document, not including the end comments;
		 */
		int getStageEnd() {
			return end.getStartPosition() - 1;
		}
	}
}
