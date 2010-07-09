package org.maven.ide.eclipse.ui.common.authentication;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.maven.ide.eclipse.authentication.internal.storage.PasswordProvider;
import org.maven.ide.eclipse.ui.common.dialogs.SecureStorageLoginDialog;

public class PasswordProviderDelegate
    extends org.maven.ide.eclipse.authentication.internal.storage.PasswordProviderDelegate
{
    public PBEKeySpec getPassword( IPreferencesContainer container, int passwordType )
    {
        final boolean newPassword = ( ( passwordType & PasswordProvider.CREATE_NEW_PASSWORD ) != 0 );
        final boolean passwordChange = ( ( passwordType & PasswordProvider.PASSWORD_CHANGE ) != 0 );

        final PBEKeySpec[] password = new PBEKeySpec[1];

        final IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                Shell shell = window != null ? window.getShell() : null;
                SecureStorageLoginDialog dialog = new SecureStorageLoginDialog( shell, newPassword, passwordChange );

                if ( dialog.open() == Dialog.OK )
                {
                    password[0] = newPassword( dialog.getPassword() );
                }
            }
        } );

        return password[0];
    }
}
