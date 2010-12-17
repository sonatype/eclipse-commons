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

package org.maven.ide.eclipse.pr.internal.sources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.about.ISystemSummarySection;
import org.maven.ide.eclipse.pr.IDataSource;
import org.maven.ide.eclipse.pr.internal.Messages;
import org.maven.ide.eclipse.pr.internal.ProblemReportingPlugin;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.ULocale;


/**
 * Returns Eclipse configuration details.
 */
public class ConfigurationDetailsSource implements IDataSource {

  public String getName() {
    return "configurationDetails.txt"; //$NON-NLS-1$
  }
  
  public InputStream getInputStream() throws CoreException {
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    writer.println(NLS.bind(Messages.ConfigurationDetailsSource_date, //
        DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date())));
    writer.println();

    appendExtensions(writer);
    
    writer.close();

    try {
      return new ByteArrayInputStream(out.toString().getBytes("UTF-8")); //$NON-NLS-1$
    } catch(UnsupportedEncodingException ex) {
      throw new CoreException(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID, -1, //
          Messages.ConfigurationDetailsSource_error, ex));
    }
  }

  private static void appendExtensions(PrintWriter writer) {
    IConfigurationElement[] configElements = getSortedExtensions();
    for(int i = 0; i < configElements.length; ++i) {
      IConfigurationElement element = configElements[i];

      writer.println(NLS.bind("*** {0}:", element.getAttribute("sectionTitle"))); //$NON-NLS-1$ //$NON-NLS-2$

      Object obj = null;
      try {
        obj = element.createExecutableExtension("class"); //$NON-NLS-1$
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      } catch (Exception ex) {
        MavenLogger.log("Cannot create extension", ex); //$NON-NLS-1$
      }

      if(obj instanceof ISystemSummarySection) {
        ISystemSummarySection logSection = (ISystemSummarySection) obj;
        logSection.write(writer);
      } else {
        writer.println("Could not write section, see error log");
      }
      
      writer.println();
    }
  }

  private static IConfigurationElement[] getSortedExtensions() {
    IConfigurationElement[] configElements = Platform.getExtensionRegistry() //
        .getConfigurationElementsFor("org.eclipse.ui.systemSummarySections"); //$NON-NLS-1$

    Arrays.sort(configElements, new Comparator<IConfigurationElement>() {
      Collator collator = Collator.getInstance(ULocale.getDefault());

      public int compare(IConfigurationElement e1, IConfigurationElement e2) {
        String id1 = e1.getAttribute("id"); //$NON-NLS-1$
        String id2 = e2.getAttribute("id"); //$NON-NLS-1$

        if(id1 != null && id2 != null && !id1.equals(id2)) {
          return collator.compare(id1, id2);
        }

        String title1 = e1.getAttribute("sectionTitle"); //$NON-NLS-1$ 
        String title2 = e2.getAttribute("sectionTitle"); //$NON-NLS-1$

        if(title1 == null) {
          title1 = ""; //$NON-NLS-1$
        }
        if(title2 == null) {
          title2 = ""; //$NON-NLS-1$
        }

        return collator.compare(title1, title2);
      }
    });

    return configElements;
  }

}
