package org.maven.ide.eclipse.swtvalidation;

import java.lang.reflect.Method;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.util.Util;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.ui.ValidationUI;
import org.openide.util.Exceptions;

public class EclipseValidationUI
{
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
