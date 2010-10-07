

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
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.ui.ValidationUI;
import org.openide.util.Exceptions;

/**
 * factory for creating basic ui to handle validation errors.
 * @author mkleint
 */
public final class SwtValidationUI {

	/**
	 * constant for setting message
	 */
	public static final int MESSAGE = 1;
	
	/**
	 * constant for enabling/disabling button
	 */
	public static final int BUTTON = 2;
	
    private SwtValidationUI() {}


    /**
     * Create ValidationUI bridging for TitleAreaDialog. Handle both error message and ok button enablement
     * @param tad
     * @return
     */
    public static ValidationUI createUI(final TitleAreaDialog tad) {
        return createUI( tad, MESSAGE | BUTTON );
    }
    
    /**
     * Create ValidationUI bridging for TitleAreaDialog.
     * @param tad
     * @param style  MESSAGE and/or BUTTON
     * @return
     */
    public static ValidationUI createUI(final TitleAreaDialog tad, final int style) {
        return new ValidationUI() {

            public void showProblem(Problem problem) {
            	if ((style & MESSAGE) != 0) {
            		tad.setMessage(problem.getMessage(), convertSeverityToMessageType(problem.severity()));
            	}
            	if ((style & BUTTON) != 0) {
            		Button ok = reflectAndConquer(tad, IDialogConstants.OK_ID );
            		if ( ok != null )
            		{
            			ok.setEnabled( !problem.isFatal() );
            		}
            	}
            }

            public void clearProblem() {
            	if ((style & MESSAGE) != 0) {
            		tad.setMessage( null );
            	}
            	if ((style & BUTTON) != 0) {
            		Button ok = reflectAndConquer(tad, IDialogConstants.OK_ID );
            		if ( ok != null )
            		{
            			ok.setEnabled( true );
            		}
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
    
    /**
     * Create ValidationUI bridging for WizardPage. Handle both error message and next button enablement
     * @param tad
     * @return
     */
    public static ValidationUI createUI( final WizardPage page) {
        return createUI( page, MESSAGE | BUTTON );
    }
    
    /**
     * Create ValidationUI bridging for WizardPage
     * @param tad
     * @param style  MESSAGE and/or BUTTON
     * @return
     */
    public static ValidationUI createUI( final WizardPage page, final int style) {
        return new ValidationUI() {
            public void showProblem(Problem problem) {
            	if ((style & MESSAGE) != 0) {
            		page.setMessage(problem.getMessage(), convertSeverityToMessageType(problem.severity()));
            	}
            	if ((style & BUTTON) != 0) {
            		page.setPageComplete(!problem.isFatal());
            	}
            }

            public void clearProblem() {
            	if ((style & MESSAGE) != 0) {
            		page.setMessage( null );
            	}
            	if ((style & BUTTON) != 0) {
            		page.setPageComplete( true );
            	}
            }
        };

    }
    
    
    /**
     * Create ValidationUI bridging for StatusDialog
     * @param tad
     * @return
     */
    public static ValidationUI createUI( final StatusDialog dialog) {
        return new ValidationUI() {
            
            public void showProblem(Problem problem) {
                IStatus status  = new Status(convertSeverityToStatusType( problem.severity() ), Policy.JFACE, problem.getMessage());
                reflectAndConquer( dialog, status );
            }

            public void clearProblem() {
                IStatus st = new Status(IStatus.OK, Policy.JFACE, IStatus.OK,
                           Util.ZERO_LENGTH_STRING, null);
                reflectAndConquer( dialog, st );
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
    
    
    /**
     * Create ValidationUI bridging for FormPage
     * @param tad
     * @return
     */
    public static ValidationUI createFormUI( final FormPage page) {
        return new ValidationUI() {
            public void showProblem(Problem problem) {
                IManagedForm managedForm = page.getManagedForm();
                if ( managedForm != null )
                {
                    managedForm.getForm().setMessage( problem.getMessage(), convertSeverityToMessageType(problem.severity()));
                }
            }

            public void clearProblem() {
                IManagedForm managedForm = page.getManagedForm();
                if ( managedForm != null )
                {
                    managedForm.getForm().setMessage( null, IMessageProvider.NONE );
                }
            }
        };
    }
    
    
    

    

    /**
     * converts problem severity to relevant IMessageProvider constant
     * @param severity
     * @return
     */
    public static int convertSeverityToMessageType(Severity severity) {
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
     * converts problem severity to relevant IStatus constant
     * @param sev
     * @return
     */
    public static int convertSeverityToStatusType(Severity sev) {
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

    //TODO check if we can remove the deprecated stuff..

    /**
     * Create ValidationUI bridging for WizardPage
     * @param tad
     * @return
     * @deprecated
     */
    public static ValidationUI createWizardPageValidationUI( final WizardPage page) {
    	return createUI( page );
    }
    
    /**
     * Create ValidationUI bridging for StatusDialog
     * @param tad
     * @return
     * @deprecated
     */
    public static ValidationUI createStatusDialogValidationUI( final StatusDialog dialog) {
    	return createUI(dialog);
    }
    
    /**
     * Create ValidationUI bridging for TitleAreaDialog.
     * @param tad
     * @return
     * @deprecated
     */
    public static ValidationUI createTitleAreaDialogValidationUI(final TitleAreaDialog tad) {
    	return createUI( tad );
    }    
    
}
