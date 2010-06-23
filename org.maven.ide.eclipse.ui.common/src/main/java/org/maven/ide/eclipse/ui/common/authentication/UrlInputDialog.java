package org.maven.ide.eclipse.ui.common.authentication;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.maven.ide.eclipse.ui.common.InputHistory;

public class UrlInputDialog
    extends TitleAreaDialog
{
    private final static int DIALOG_WIDTH = 600;

    private String title;

    private String urlLabelText;

    private UrlInputComposite urlComposite;

    private String url;

    private int inputStyle;

    protected String errorMessage;

    private InputHistory initialHistory;

    public UrlInputDialog( Shell parentShell, String title, String urlLabelText )
    {
        this( parentShell, title, urlLabelText, null, UrlInputComposite.ALLOW_ANONYMOUS );
    }

    public UrlInputDialog( Shell parentShell, String title, String urlLabelText, String url )
    {
        this( parentShell, title, urlLabelText, url, UrlInputComposite.ALLOW_ANONYMOUS
            | UrlInputComposite.READ_ONLY_URL );
    }

    public UrlInputDialog( Shell parentShell, String title, String urlLabelText, String url, int inputStyle )
    {
        super( parentShell );
        assert title != null;
        assert urlLabelText != null;
        setShellStyle( SWT.CLOSE | SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL );
        this.title = title;
        this.urlLabelText = urlLabelText;
        this.url = url;
        this.inputStyle = inputStyle;
    }

    @Override
    protected void configureShell( Shell shell )
    {
        super.configureShell( shell );
        shell.setText( title );
    }

    @Override
    protected void buttonPressed( int buttonId )
    {
        url = buttonId == IDialogConstants.OK_ID ? urlComposite.getUrl() : null;

        super.buttonPressed( buttonId );
    }

    public String getUrl()
    {
        return url;
    }

    public final void setInputHistory( InputHistory history )
    {
        if ( urlComposite == null )
        {
            initialHistory = history;
        }
        else
        {
            urlComposite.setInputHistory( history );
        }
    }

    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite composite = (Composite) super.createDialogArea( parent );
        composite.setLayout( new GridLayout( 1, true ) );

        setMessage( title );
        setErrorMessage( errorMessage );

        urlComposite = new UrlInputComposite( composite, urlLabelText, url, inputStyle )
        {
            @Override
            public String validate()
            {
                String message = super.validate();
                setErrorMessage( message );
                Button ok = getButton( IDialogConstants.OK_ID );
                if ( ok != null )
                {
                    ok.setEnabled( message == null );
                }
                return message;
            }
        };
        if ( initialHistory != null )
        {
            urlComposite.setInputHistory( initialHistory );
        }

        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, false );
        gd.widthHint = DIALOG_WIDTH;
        urlComposite.setLayoutData( gd );

        applyDialogFont( composite );
        return composite;
    }

    public void setErrorText( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }

    @Override
    protected Control createButtonBar( Composite parent )
    {
        Control control = super.createButtonBar( parent );
        getButton( IDialogConstants.OK_ID ).setEnabled( urlComposite.validate() == null );
        urlComposite.setFocus();
        return control;
    }
    
    @Override
    protected Point getInitialSize()
    {
        return getShell().computeSize( SWT.DEFAULT, SWT.DEFAULT, true );
    }
}
