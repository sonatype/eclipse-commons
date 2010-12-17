/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.wizard;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.swizzle.IssueSubmissionRequest;
import org.codehaus.plexus.swizzle.IssueSubmissionResult;
import org.codehaus.plexus.swizzle.IssueSubmitter;
import org.codehaus.plexus.swizzle.JiraIssueSubmitter;
import org.codehaus.plexus.swizzle.jira.authentication.DefaultAuthenticationSource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.maven.ide.eclipse.pr.internal.Messages;
import org.maven.ide.eclipse.pr.internal.data.Data;
import org.maven.ide.eclipse.pr.internal.data.DataGatherer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;


/**
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class ProblemReportingWizard extends Wizard implements IImportWizard {

  private IStructuredSelection selection;

  private static final String HOSTNAME = "issues.sonatype.org/"; //$NON-NLS-1$

  private static final String URL = "https://" + HOSTNAME; //$NON-NLS-1$

  private static final String USERNAME = "sonatype_problem_reporting"; //$NON-NLS-1$

  private static final String PASSWORD = "sonatype_problem_reporting"; //$NON-NLS-1$

  private static final String PROJECT = "PR"; //$NON-NLS-1$

  protected static String TITLE = Messages.ProblemReportingWizard_title;

  //private ProblemReportingSelectionPage selectionPage;

  private ProblemDescriptionPage descriptionPage;

  public ProblemReportingWizard() {
    setWindowTitle(Messages.ProblemReportingWizard_window_title);
  }

  @Override
  public void addPages() {
    descriptionPage = new ProblemDescriptionPage(selection);
    addPage(descriptionPage);
    //selectionPage = new ProblemReportingSelectionPage();
    //addPage(selectionPage);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
  }

  public boolean performFinish() {
    final Set<Data> dataSet = new HashSet<Data>();//selectionPage.getDataSet();
    dataSet.addAll(EnumSet.allOf(Data.class));
    dataSet.remove(Data.MAVEN_POM_FILES);
//    if(locationFile.exists()) {
//      if(!MessageDialog.openQuestion(getShell(), "File already exists", //
//          "File " + location + " already exists.\nDo you want to overwrite?")) {
//        return false;
//      }
//      if(!locationFile.delete()) {
//        MavenLogger.log("Can't delete file " + location, null);
//      }
//    }

    new Job(Messages.ProblemReportingWizard_job_gathering) {
      protected IStatus run(IProgressMonitor monitor) {
        List<File> bundleFiles = null;
        try {
          String tmpPath = ResourcesPlugin.getPlugin().getStateLocation().toOSString();
          File tmpDir = new File(tmpPath);
          bundleFiles = saveData(tmpDir, dataSet, monitor);
          IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
          String username = mavenConfiguration.getJiraUsername();
          String password = mavenConfiguration.getJiraPassword();
          if(username == null || username.trim().equals("")) { //$NON-NLS-1$
            username = USERNAME;
            password = PASSWORD;
          }

          IssueSubmitter is = new JiraIssueSubmitter(URL, new DefaultAuthenticationSource(username, password));

          IssueSubmissionRequest r = new IssueSubmissionRequest();
          r.setProjectId(PROJECT);
          r.setSummary(descriptionPage.getProblemSummary());
          r.setDescription(descriptionPage.getProblemDescription());
          r.setReporter(username);
          r.setEnvironment(getEnvironment());

          //
          // Problem Report Bundles
          //
          for (File bundleFile : bundleFiles) {
            r.addProblemReportBundle(bundleFile);
          }
          
          //
          // Screen Captures
          //
          if(descriptionPage.getScreenCapture() != null && descriptionPage.getScreenCapture().isFile()) {
            r.addScreenCapture(descriptionPage.getScreenCapture());
          }          

          IssueSubmissionResult res = is.submitIssue(r);

          showHyperlink(Messages.ProblemReportingWizard_link_success, res.getIssueUrl());
        } catch(Exception ex) {
          MavenLogger.log("Failed to generate problem report", ex);
          showError((ex.getMessage() != null) ? ex.getMessage() : ex.toString());
        } finally {
          if(bundleFiles != null) {
            for(File bundleFile : bundleFiles) {
              bundleFile.delete();
            }
          }
        }

        return Status.OK_STATUS;
      }
      

      private void showError(final String msg) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), //
                TITLE, msg);
          }
        });
      }

      private void showHyperlink(final String msg, final String url) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            HyperlinkDialog dialog = new HyperlinkDialog(Display.getCurrent().getActiveShell(), TITLE, null, msg,
                MessageDialog.INFORMATION, new String[] {IDialogConstants.OK_LABEL}, 0, url);
            dialog.open();
          }
        });
      }
    }.schedule();

    return true;
  }

  private static final String [] PROPERTIES=new String[]{"java.vendor","java.version","os.name","os.version","os.arch","osgi.arch","osgi.nl"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
  
  private String getEnvironment() {
    StringBuffer sb = new StringBuffer();
    
    String sep = System.getProperty("line.separator"); //$NON-NLS-1$
    
    sb.append("M2E Version: ").append(getBundleVersion(MavenPlugin.getDefault().getBundle())).append(sep);
    sb.append("Eclipse Version: ").append(getBundleVersion(ResourcesPlugin.getPlugin().getBundle())).append(sep);
    
    for(int i = 0; i < PROPERTIES.length; i++ ) {
      sb.append(PROPERTIES[i]).append(": ").append(System.getProperty(PROPERTIES[i])).append(sep);
    }
    
    
    return sb.toString();
  }
  
  private String getBundleVersion(Bundle bundle) {
    String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    Version v = org.osgi.framework.Version.parseVersion(version);
    return v.toString();
  }
  
  List<File> saveData(File bundleDir, Set<Data> dataSet, IProgressMonitor monitor) throws IOException {
    //refresh the projects in case they're out of date
    Set<IProject> projects = new LinkedHashSet<IProject>();
    List<?> list = descriptionPage.getSelectedProjects().toList();
    int numTicks = list == null ? 0 : list.size();
    SubProgressMonitor sub = new SubProgressMonitor(monitor, numTicks);
    sub.beginTask(Messages.ProblemReportingWizard_monitor_reading, numTicks);
    for(Iterator<?> i = descriptionPage.getSelectedProjects().iterator(); i.hasNext();) {
      try{
      Object o = i.next();
        if(o instanceof JavaProject) {
          
          JavaProject jp = (JavaProject)o;
          projects.add(jp.getProject());
          if(jp.getResource() != null){
            jp.getResource().refreshLocal(IResource.DEPTH_INFINITE, sub);
          }
          
        }else if(o instanceof IProject) {
          IProject project = (IProject)o;
          projects.add(project);
          if(project.getWorkspace() != null && project.getWorkspace().getRoot() != null){
            project.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, sub);
          }
          
        }
      } catch(CoreException e){
        MavenLogger.log(e);
      }
      sub.worked(1);
    }
    sub.done();
    
    MavenPlugin mavenPlugin = MavenPlugin.getDefault();
    DataGatherer gatherer = new DataGatherer(MavenPlugin.getDefault().getMavenConfiguration(), //
        mavenPlugin.getMavenProjectManager(), mavenPlugin.getConsole(), //
        ResourcesPlugin.getWorkspace(), projects, getClass().getResource("/apr/public-key.txt")); //$NON-NLS-1$

    return gatherer.gather(bundleDir, dataSet, monitor);
  }

}
