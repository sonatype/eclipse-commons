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

package org.maven.ide.eclipse.pr.internal;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.m2e.core.actions.AbstractMavenMenuCreator;
import org.eclipse.m2e.core.actions.SelectionUtil;
import org.maven.ide.eclipse.pr.internal.wizard.ProblemReportingAction;


public class ProblemReportingMenuCreator extends AbstractMavenMenuCreator {

  public void createMenu(IMenuManager mgr) {
    int selectionType = SelectionUtil.getSelectionType(selection);
    if(selectionType == SelectionUtil.PROJECT_WITH_NATURE || selectionType == SelectionUtil.PROJECT_WITHOUT_NATURE) {
      mgr.appendToGroup(IMPORT, new Separator()); //
      mgr.appendToGroup(IMPORT, getAction(new ProblemReportingAction(), //
          ProblemReportingAction.ID, Messages.ProblemReportingMenuCreator_action_report, ProblemReportingImages.REPORT_BUG));
    }
  }

}
