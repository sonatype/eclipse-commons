/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonatype.simplevalidation.swt;

import java.lang.reflect.Method;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Button;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.ui.ValidationUI;
import org.openide.util.Exceptions;

/**
 *
 * @author mkleint
 */
public final class SwtValidationUI {

    private SwtValidationUI() {}


    public static ValidationUI createTitleAreaDialogValidationUI(final TitleAreaDialog tad) {
        return new ValidationUI() {

            @Override
            public void showProblem(Problem problem) {
                tad.setMessage(problem.getMessage(), convertSeverityToMessageType(problem.severity()));
                Button ok = reflectAndConquer(tad, IDialogConstants.OK_ID );
                if ( ok != null )
                {
                    ok.setEnabled( !problem.isFatal() );
                }
            }

            @Override
            public void clearProblem() {
                tad.setMessage( null );
                Button ok = reflectAndConquer(tad, IDialogConstants.OK_ID );
                if ( ok != null )
                {
                    ok.setEnabled( true );
                }
            }

            private Button reflectAndConquer(TitleAreaDialog tad, int button)
            {
                try {
                    Method m = Dialog.class.getDeclaredMethod("getButton", Integer.TYPE);
                    m.setAccessible(true);
                    return (Button) m.invoke(tad, button);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    return null;
                }
            }
        };
    }

    private static int convertSeverityToMessageType(Severity severity) {
        if (Severity.FATAL.equals(severity)) {
            return IMessageProvider.ERROR;
        } else if (Severity.WARNING.equals(severity)) {
            return IMessageProvider.WARNING;
        } else if (Severity.INFO.equals(severity)) {
            return IMessageProvider.INFORMATION;
        }
        return IMessageProvider.NONE;
    }

    public static ValidationUI createWizardPageValidationUI( final WizardPage page) {
        return new ValidationUI() {
            @Override
            public void showProblem(Problem problem) {
                page.setMessage(problem.getMessage(), convertSeverityToMessageType(problem.severity()));
                page.setPageComplete(!problem.isFatal());
            }

            @Override
            public void clearProblem() {
                page.setMessage( null );
                page.setPageComplete( true );
            }
        };
    }
}
