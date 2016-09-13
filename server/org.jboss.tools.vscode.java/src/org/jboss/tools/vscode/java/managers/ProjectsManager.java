package org.jboss.tools.vscode.java.managers;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.vscode.java.JavaLanguageServerPlugin;

public class ProjectsManager {
	
	public enum CHANGE_TYPE { CREATED, CHANGED, DELETED};

	private static final String TMP_PROJECT_NAME = "tmpProject";

	public IProject getCurrentProject() {
		return getWorkspace().getRoot().getProject(TMP_PROJECT_NAME);
	}

	public IStatus createProject(final String projectName, List<IProject> resultingProjects, IProgressMonitor monitor) {
		try {
			IProjectImporter importer = getImporter(new File(projectName), monitor);
			if (importer == null) {
				return Status.OK_STATUS;
			}
			List<IProject> projects = importer.importToWorkspace(monitor);
			
			List<IProject> javaProjects = projects.stream().filter(p -> {
				try {
					return p.hasNature(JavaCore.NATURE_ID);
				} catch (CoreException e) {
					return false;
				}
			}).collect(Collectors.toList());
			
			JavaLanguageServerPlugin.logInfo("Number of created projects " + javaProjects.size());
			resultingProjects.addAll(javaProjects);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			JavaLanguageServerPlugin.logException("Problem importing to workspace", e);
			return new Status(IStatus.ERROR, "", "Import failed: " + e.getMessage());
		} catch (InterruptedException e) {
			JavaLanguageServerPlugin.logInfo("Import cancelled");
			return Status.CANCEL_STATUS;
		}
	}

	private IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public void fileChanged(String uri, CHANGE_TYPE changeType) {
		try {
			this.getCurrentProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			JavaLanguageServerPlugin.logException("Problem refreshing workspace", e);
		}
	}
	
	private IProjectImporter getImporter(File rootFolder, IProgressMonitor monitor) throws InterruptedException, CoreException {
		for (IProjectImporter importer : importers()) {
			importer.initialize(rootFolder);
			if (importer.applies(monitor)) {
				return importer;
			}
		}
		return null;
	}
	
	private Collection<IProjectImporter> importers() {
		//TODO read extension point
		return Arrays.asList(new MavenProjectImporter(), new EclipseProjectImporter());
	}

}
