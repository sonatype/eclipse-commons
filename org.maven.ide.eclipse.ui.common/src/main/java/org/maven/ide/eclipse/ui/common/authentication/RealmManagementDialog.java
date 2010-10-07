package org.maven.ide.eclipse.ui.common.authentication;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.Activator;
import org.maven.ide.eclipse.ui.common.Messages;

public class RealmManagementDialog
    extends WizardDialog
{
    public RealmManagementDialog( Shell parentShell )
    {
        super( parentShell, new Wizard()
        {
            private RealmManagementPage realmManagementPage;

            @Override
            public boolean performFinish()
            {
                Throwable t = null;
                try
                {
                    getContainer().run( true, true, new IRunnableWithProgress()
                    {
                        public void run( IProgressMonitor monitor )
                            throws InvocationTargetException, InterruptedException
                        {
                            realmManagementPage.save( monitor );
                            monitor.done();
                        }
                    } );
                }
                catch ( InvocationTargetException e )
                {
                    t = e.getTargetException();
                }
                catch ( InterruptedException e )
                {
                    t = e;
                }

                if ( t == null )
                {
                    realmManagementPage.setMessage( null, IMessageProvider.ERROR );
                    realmManagementPage.refreshSelection();
                }
                else
                {
                    String message = NLS.bind( Messages.realmManagementDialog_errorSavingRealm, t.getMessage() );
                    realmManagementPage.setMessage( message, IMessageProvider.ERROR );
                    StatusManager.getManager().handle( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, t ) );
                }
                return false;
            }

            @Override
            public void addPages()
            {
                realmManagementPage = new RealmManagementPage();
                addPage( realmManagementPage );
                setWindowTitle( Messages.realmManagementDialog_title );
                setNeedsProgressMonitor( true );
            }
        } );
    }

    @Override
    protected void createButtonsForButtonBar( Composite parent )
    {
        super.createButtonsForButtonBar( parent );
        Button finish = getButton( IDialogConstants.FINISH_ID );
        finish.setText( Messages.realmManagementDialog_apply );
        Button cancel = getButton( IDialogConstants.CANCEL_ID );
        cancel.setText( IDialogConstants.CLOSE_LABEL );
    }
}
