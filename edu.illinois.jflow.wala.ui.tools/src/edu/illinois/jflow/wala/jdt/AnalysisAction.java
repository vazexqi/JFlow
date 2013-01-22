package edu.illinois.jflow.wala.jdt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.ibm.wala.ide.AbstractJavaAnalysisAction;

public class AnalysisAction extends AbstractJavaAnalysisAction {

	public AnalysisAction() {
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			computeScope((IStructuredSelection)getCurrentSelection());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
