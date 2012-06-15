package edu.illinois.jflow.wala.ui.tools.graph.jdt.util;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class JavaEditorUtil {
	public static JavaEditor getActiveJavaEditor() {
		IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor instanceof JavaEditor) {
			return (JavaEditor)activeEditor;
		}
		return null;
	}
}
