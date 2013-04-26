package edu.illinois.jflow.core.transformations.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Represents an annotated stage in the source code.
 * 
 * This class provides functionality to get the location (start and end offset) of the annotated
 * stage.
 * 
 * @author nchen
 * 
 */
@SuppressWarnings("restriction")
public final class AnnotatedStage {
	Integer stageName;

	IDocument doc;

	Comment start;

	Comment end;

	public AnnotatedStage(Integer stageName, IDocument doc) {
		this.doc= doc;
		this.stageName= stageName;
	}

	/*
	 * Assumes first comment to be added is the start comment 
	 */
	public void addComment(Comment comment) {
		if (start == null) {
			start= comment;
		}
		else if (end == null) {
			end= comment;
		} else {
			throw new IllegalStateException("Already have begin and end comments");
		}
	}

	public List<Integer> getStageLines() {
		List<Integer> lines= new ArrayList<Integer>();
		try {
			// IDocument starts counting from 0 but we want to follow what the user sees in the editor
			// that starts from 1.
			int start= doc.getLineOfOffset(getStageStartOffset()) + 1;
			int end= doc.getLineOfOffset(getStageEndOffset()) + 1;

			// Grab everything including START and END
			for (int line= start; line <= end; line++) {
				lines.add(line);
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return lines;
	}

	/*
	 * Returns a Selection object representing the current stage
	 */
	public Selection getSelection() {
		return Selection.createFromStartEnd(getStageStartOffset(), getStageEndOffset());
	}

	/*
	 * Returns the offset in the document, including the start comments
	 */
	private int getStageStartOffset() {
		return start.getStartPosition();
	}

	/*
	 * Returns the offset in the document, including the end comments;
	 */
	private int getStageEndOffset() {
		return end.getStartPosition() + end.getLength();
	}


}
