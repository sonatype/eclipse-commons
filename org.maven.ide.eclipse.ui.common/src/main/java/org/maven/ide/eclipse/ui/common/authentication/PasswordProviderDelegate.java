package org.maven.ide.eclipse.ui.common.authentication;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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

        final Display display = getDisplay();
        display.syncExec( new Runnable()
        {
            public void run()
            {
                Shell shell = display.getActiveShell();
                SecureStorageLoginDialog dialog = new SecureStorageLoginDialog( shell, newPassword, passwordChange );

                if ( dialog.open() == Dialog.OK )
                {
                    password[0] = newPassword( dialog.getPassword() );
                }
            }
        } );

        return password[0];
    }

    protected Display getDisplay()
    {
        Display display = Display.getCurrent();

        // if ( display == null )
        // {
        // Thread currentThread = Thread.currentThread();
        // Class<? extends Thread> mctClass = currentThread.getClass();
        // if ( "org.eclipse.jface.operation.ModalContext$ModalContextThread".equals( mctClass.getName() ) )
        // {
        // try
        // {
        // Field displayField = mctClass.getDeclaredField( "display" );
        // boolean origAccessible = displayField.isAccessible();
        // displayField.setAccessible( true );
        // try
        // {
        // display = (Display) displayField.get( currentThread );
        // }
        // finally
        // {
        // displayField.setAccessible( origAccessible );
        // }
        // }
        // catch ( Exception e )
        // {
        // // sigh.
        // }
        // }
        // }

        if ( display == null )
        {
            display = Display.getDefault();
        }

        return display;
    }
}
