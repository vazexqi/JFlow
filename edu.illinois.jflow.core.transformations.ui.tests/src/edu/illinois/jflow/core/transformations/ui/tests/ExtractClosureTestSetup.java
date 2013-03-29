package edu.illinois.jflow.core.transformations.ui.tests;

import junit.framework.Test;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

@SuppressWarnings("restriction")
public class ExtractClosureTestSetup extends RefactoringTestSetup {

	private IPackageFragment basicPackage;

	public ExtractClosureTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		basicPackage= root.createPackageFragment("basic_in", true, null);
	}

	public IPackageFragment getBasicPackage() {
		return basicPackage;
	}

	protected IPackageFragmentRoot addRTJar(IJavaProject project) throws CoreException {
		JavaProjectHelper.set16CompilerOptions(project);

		Path path= new Path("org.eclipse.jdt.launching.JRE_CONTAINER");
		IClasspathEntry cpe= JavaCore.newContainerEntry(path);
		JavaProjectHelper.addToClasspath(project, cpe);
		IResource workspaceResource= ResourcesPlugin.getWorkspace().getRoot().findMember(cpe.getPath());
		if (workspaceResource != null) {
			return project.getPackageFragmentRoot(workspaceResource);
		}
		return project.getPackageFragmentRoot(path.toString());
	}
}
