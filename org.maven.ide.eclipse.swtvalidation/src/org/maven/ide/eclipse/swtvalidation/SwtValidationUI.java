

package org.maven.ide.eclipse.swtvalidation;

import java.lang.reflect.Method;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.util.Util;
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


    /**
     * Create ValidationUI bridging for TitleAreaDialog.
     * @param tad
     * @return
     */
    public static ValidationUI createTitleAreaDialogValidationUI(final TitleAreaDialog tad) {
        return new ValidationUI() {

            public void showProblem(Problem problem) {
                tad.setMessage(problem.getMessage(), convertSeverityToMessageType(problem.severity()));
                Button ok = reflectAndConquer(tad, IDialogConstants.OK_ID );
                if ( ok != null )
                {
                    ok.setEnabled( !problem.isFatal() );
                }
            }

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

    /**
     * Create ValidationUI bridging for WizardPage
     * @param tad
     * @return
     */
    public static ValidationUI createWizardPageValidationUI( final WizardPage page) {
        return new ValidationUI() {
            public void showProblem(Problem problem) {
                page.setMessage(problem.getMessage(), convertSeverityToMessageType(problem.severity()));
                page.setPageComplete(!problem.isFatal());
            }

            public void clearProblem() {
                page.setMessage( null );
                page.setPageComplete( true );
            }
        };
    }
    
    /**
     * Create ValidationUI bridging for StatusDialog
     * @param tad
     * @return
     */
    public static ValidationUI createStatusDialogValidationUI( final StatusDialog dialog) {
        return new ValidationUI() {
            
            public void showProblem(Problem problem) {
                IStatus status  = new Status(getStatusLevel(problem.severity()), Policy.JFACE, problem.getMessage());
                reflectAndConquer( dialog, status );
            }

            public void clearProblem() {
                IStatus st = new Status(IStatus.OK, Policy.JFACE, IStatus.OK,
                           Util.ZERO_LENGTH_STRING, null);
                reflectAndConquer( dialog, st );
            }
            
            private int getStatusLevel(Severity sev) {
                if (Severity.INFO == sev) {
                    return IStatus.INFO;
                }
                if (Severity.WARNING == sev) {
                    return IStatus.WARNING;
                }
                if (Severity.FATAL == sev) {
                    return IStatus.ERROR;
                }
                return IStatus.OK;
            }
            
            private void reflectAndConquer(StatusDialog dialog, IStatus status)
            {
                try {
                    Method m = StatusDialog.class.getDeclaredMethod("updateStatus", IStatus.class);
                    m.setAccessible(true);
                    m.invoke(dialog, status);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            
        };
    }  
    
}
