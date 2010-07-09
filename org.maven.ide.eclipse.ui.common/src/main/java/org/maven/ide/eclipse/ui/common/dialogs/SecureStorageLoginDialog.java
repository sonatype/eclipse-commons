package org.maven.ide.eclipse.ui.common.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.ui.common.Messages;

public class SecureStorageLoginDialog
    extends TitleAreaDialog
    implements ModifyListener
{
    private Text password;

    private Text password2;

    private String passwordStr;

    private final boolean newPassword;

    public SecureStorageLoginDialog( Shell parentShell, boolean newPassword, boolean passwordChange )
    {
        super( parentShell );
        this.newPassword = newPassword;
    }

    protected void configureShell( Shell shell )
    {
        super.configureShell( shell );
        shell.setText( Messages.secureStorageDialog_title );
    }

    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite dialogArea = (Composite) super.createDialogArea( parent );

        setTitle( Messages.secureStorageDialog_title );
        setMessage( newPassword ? Messages.secureStorageDialog_messageLoginChange
                        : Messages.secureStorageDialog_messageLogin );

        Composite composite = new Composite( dialogArea, SWT.NONE );
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, false ) );
        composite.setLayout( new GridLayout( 2, false ) );

        Label label = new Label( composite, SWT.NONE );
        label.setText( Messages.secureStorageDialog_label_password );

        password = new Text( composite, SWT.BORDER | SWT.PASSWORD );
        password.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );
        password.addModifyListener( this );

        if ( newPassword )
        {
            Label label2 = new Label( composite, SWT.NONE );
            label2.setText( Messages.secureStorageDialog_label_confirm );

            password2 = new Text( composite, SWT.BORDER | SWT.PASSWORD );
            password2.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );
            password2.addModifyListener( this );
        }

        return dialogArea;
    }

    @Override
    protected Control createButtonBar( Composite parent )
    {
        Control control = super.createButtonBar( parent );
        getButton( IDialogConstants.OK_ID ).setEnabled( false );
        password.setFocus();
        return control;
    }

    public String getPassword()
    {
        return passwordStr;
    }

    public void modifyText( ModifyEvent e )
    {
        validate();
    }

    private void validate()
    {
        String message = null;

        if ( password.getText().length() == 0 )
        {
            message = Messages.secureStorageDialog_errors_emptyPassword;
        }
        else if ( password2 != null && !password.getText().equals( password2.getText() ) )
        {
            message = Messages.secureStorageDialog_errors_noMatch;
        }
        else
        {
            passwordStr = password.getText();
        }

        getButton( IDialogConstants.OK_ID ).setEnabled( message == null );
        setErrorMessage( message );
    }
}
