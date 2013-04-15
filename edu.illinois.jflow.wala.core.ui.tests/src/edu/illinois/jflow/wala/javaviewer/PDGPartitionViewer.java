package edu.illinois.jflow.wala.javaviewer;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.viz.viewer.WalaViewer;

import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

/*
 * This class doesn't really test anything. I'm just abusing the infrastructure that we have set up
 * so we can launch a GUI to visualize where the pointers are.
 */
public class PDGPartitionViewer extends JFlowTest {
	private static final String PROJECT_NAME= "edu.illinois.jflow.test.data";

	private static final String PROJECT_ZIP= "test-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public PDGPartitionViewer() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "pdg";
	}

	//////////
	// Tests
	// There is a special naming convention here that must be obeyed
	// The name of the method corresponds to the Java class file that we want to test
	// i.e., testBlah looks for a class Blah

	@Test
	public void testProject5() throws IllegalArgumentException, IOException, CancelException, InterruptedException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		Shell shell= new Shell(Display.getDefault());
		(new Dialog(shell) {
			public void open() {
				Shell parent= getParent();
				Shell shell= new Shell(parent, SWT.EMBEDDED | SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
				shell.setSize(600, 800);

				Frame frame= SWT_AWT.new_Frame(shell);
				frame.setSize(600, 800);
				frame.setLayout(new BorderLayout());
				frame.add(new WalaViewer(callGraph, engine.getPointerAnalysis()), BorderLayout.CENTER);
				frame.pack();
				frame.setVisible(true);

				shell.pack();
				shell.open();
				Display display= parent.getDisplay();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}
			}
		}).open();
	}
}
