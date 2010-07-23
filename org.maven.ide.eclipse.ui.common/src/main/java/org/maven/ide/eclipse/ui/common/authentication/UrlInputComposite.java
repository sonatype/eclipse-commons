package org.maven.ide.eclipse.ui.common.authentication;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.ui.common.InputHistory;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;

/**
 * Reusable widget to enter a URL and/or supply the associated credentials.
 * <P/>
 * When the widget is initially displayed, it checks if the auth registry already contains credentials for the given
 * realm or URL, and populates the username/password fields. The URL can be rendered read-only or editable, a drop-down
 * input history is provided. If the URL string does not contain a protocol, it's displayed as read-only and used as a
 * realm id.
 * <P/>
 * The component is ready to be used in dialogs and wizard pages. You may override the validate() method if you need to
 * update the error message in the wizard or do additional validations.
 * <P/>
 * Once the wizard flow is complete, call the getUrl() method to retrieve the URL. The credentials will be automatically
 * saved in the auth registry.
 */
public class UrlInputComposite
    extends Composite
{
    public static final int READ_ONLY_URL = 1;

    public static final int ALLOW_ANONYMOUS = 2;

    public static final int CERTIFICATE_CONTROLS = 4;

    private static final int INPUT_WIDTH = 200;

    private static final int INPUT_INDENT = 10;

    private static final String SETTINGS = "urls";

    private UrlFieldFacade urlComponent;

    protected String url;

    private Text usernameText;

    private Text passwordText;

    protected boolean updating = false;

    private boolean readonlyUrl = true;

    private boolean displayCertificateControls;

    private InputHistory inputHistory;

    private ModifyListener modifyListener;

    private String urlLabelText;

    private boolean allowAnonymous;

    private Button useCertificateButton;

    private Label certificateLabel;

    private Text certificateText;

    private Button browseCertificateButton;

    private Label passphraseLabel;

    private Text passphraseText;

    private WidthGroup widthGroup;

    public UrlInputComposite( Composite parent, String urlLabelText )
    {
        this( parent, urlLabelText, null, ALLOW_ANONYMOUS );
    }

    /**
     * @wbp.parser.constructor
     */
    public UrlInputComposite( Composite parent, String urlLabelText, String url, int style )
    {
        super( parent, SWT.NONE );
        this.readonlyUrl = ( style & READ_ONLY_URL ) != 0;
        this.allowAnonymous = ( style & ALLOW_ANONYMOUS ) != 0;
        this.displayCertificateControls = ( style & CERTIFICATE_CONTROLS ) != 0;
        this.url = url;
        this.urlLabelText = urlLabelText;
        if ( readonlyUrl )
        {
            assert url != null;
        }

        widthGroup = new WidthGroup();

        setInputHistory( new InputHistory( SETTINGS ) );

        setLayout( new GridLayout( 4, false ) );
        // 1.create controls
        createControls( urlLabelText, url );

        // 2. set fields.
        if ( url != null )
        {
            urlComponent.setText( url );
            updateCredentials();
        }
        else if ( !readonlyUrl )
        {
            Combo combo = getComboBoxComponent();
            String[] values = combo.getItems();
            if ( values.length > 0 )
            {
                combo.setText( values[0] );
                updateCredentials();
            }
        }

        // 3. add change listeners
        urlComponent.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                updateCredentials();
                validate();
            }
        } );
        urlComponent.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                updateCredentials();
                validate();
            }
        } );

        modifyListener = new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                if ( !updating )
                {
                    validate();
                }
            }
        };
        usernameText.addModifyListener( modifyListener );
        passwordText.addModifyListener( modifyListener );

        setTabList( new Control[] { urlComponent.getWrappedControl(), usernameText, passwordText } );
    }

    public void setInputHistory( InputHistory hist )
    {
        assert hist != null;
        inputHistory = hist;
        if ( urlComponent != null )
        {
            // setting different iput history after the ui component hierarchy was constructed
            Control c = urlComponent.getWrappedControl();
            if ( c instanceof Combo )
            {
                inputHistory.add( urlLabelText, (Combo) c );
                inputHistory.load();
            }
        }
    }

    protected void createControls( String urlLabelText, String url )
    {
        Label urlLabel = new Label( this, SWT.NONE );
        urlLabel.setText( urlLabelText );
        urlLabel.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        widthGroup.addControl( urlLabel );

        createUrlControl( urlLabelText );

        createCredentialControls();
    }

    protected void createCredentialControls()
    {
        Label usernameLabel = new Label( this, SWT.NONE );
        usernameLabel.setText( Messages.urlInput_username );
        GridData usernameLabelData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        usernameLabelData.horizontalIndent = INPUT_INDENT;
        usernameLabel.setLayoutData( usernameLabelData );
        widthGroup.addControl( usernameLabel );

        usernameText = new Text( this, SWT.BORDER );
        GridData usernameGridData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        usernameGridData.widthHint = INPUT_WIDTH;
        usernameText.setLayoutData( usernameGridData );

        Label anonymousLabel = new Label( this, SWT.NONE );
        anonymousLabel.setText( allowAnonymous ? Messages.urlInput_anonymousIfEmpty : "" );
        anonymousLabel.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 2, 1 ) );

        Label passwordLabel = new Label( this, SWT.NONE );
        passwordLabel.setText( Messages.urlInput_password );
        GridData passwordLabelData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        passwordLabelData.horizontalIndent = INPUT_INDENT;
        passwordLabel.setLayoutData( passwordLabelData );
        widthGroup.addControl( passwordLabel );

        passwordText = new Text( this, SWT.BORDER | SWT.PASSWORD );
        passwordText.setLayoutData( usernameGridData );

        if ( displayCertificateControls )
        {
            useCertificateButton = new Button( this, SWT.CHECK );
            useCertificateButton.setData( "name", "useCertificateButton" );
            GridData useCertificateButtonData = new GridData( SWT.LEFT, SWT.CENTER, false, false, 4, 1 );
            useCertificateButtonData.horizontalIndent = INPUT_INDENT;
            useCertificateButton.setLayoutData( useCertificateButtonData );
            useCertificateButton.setText( Messages.urlInput_useCertificate );
            useCertificateButton.addSelectionListener( new SelectionAdapter()
            {
                @Override
                public void widgetSelected( SelectionEvent e )
                {
                    if ( updating )
                    {
                        return;
                    }

                    if ( useCertificateButton.getSelection() )
                    {
                        createCertificateControls();
                        updateCredentials();
                    }
                    else
                    {
                        removeCertificateControls();
                    }

                    getShell().pack();
                    validate();
                }
            } );
        }
    }

    protected void createUrlControl( String urlLabelText )
    {
        UrlFieldFacade facade;
        if ( url != null && readonlyUrl )
        {
            try
            {
                new URL( url );
                final Text urlText = new Text( this, SWT.READ_ONLY );
                GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 );
                gd.widthHint = 100;
                urlText.setLayoutData( gd );
                urlText.setBackground( urlText.getDisplay().getSystemColor( SWT.COLOR_WIDGET_BACKGROUND ) );
                facade = new UrlFieldFacade()
                {

                    public void setText( String text )
                    {
                        urlText.setText( text );
                    }

                    public void setEnabled( boolean enabled )
                    {
                    }

                    public String getText()
                    {
                        return urlText.getText();
                    }

                    public void addModifyListener( ModifyListener listener )
                    {
                    }

                    public void addSelectionListener( SelectionListener listener )
                    {
                    }

                    public Control getWrappedControl()
                    {
                        return urlText;
                    }
                };
            }
            catch ( MalformedURLException e1 )
            {
                final Label realmIdLabel = new Label( this, SWT.NONE );
                GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 );
                gd.widthHint = 100;
                realmIdLabel.setLayoutData( gd );
                facade = new UrlFieldFacade()
                {
                    public void setText( String text )
                    {
                        realmIdLabel.setText( text );
                    }

                    public void setEnabled( boolean enabled )
                    {
                    }

                    public String getText()
                    {
                        return realmIdLabel.getText();
                    }

                    public void addModifyListener( ModifyListener listener )
                    {
                    }

                    public void addSelectionListener( SelectionListener listener )
                    {
                    }

                    public Control getWrappedControl()
                    {
                        return realmIdLabel;
                    }
                };
            }
        }
        else
        {
            final Combo combo = new Combo( this, SWT.NONE );
            combo.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 ) );

            inputHistory.add( urlLabelText, combo );
            inputHistory.load();

            facade = new UrlFieldFacade()
            {
                public void setText( String text )
                {
                    combo.setText( text );
                }

                public void setEnabled( boolean enabled )
                {
                    combo.setEnabled( enabled );
                }

                public String getText()
                {
                    return combo.getText();
                }

                public void addModifyListener( ModifyListener listener )
                {
                    combo.addModifyListener( listener );
                }

                public void addSelectionListener( SelectionListener listener )
                {
                    combo.addSelectionListener( listener );
                }

                public Control getWrappedControl()
                {
                    return combo;
                }
            };

        }
        // use variable to be sure the field is set.
        assert facade != null;
        urlComponent = facade;
    }

    private void removeCertificateControls()
    {
        certificateLabel.dispose();
        certificateText.dispose();
        browseCertificateButton.dispose();
        passphraseLabel.dispose();
        passphraseText.dispose();
    }

    private void createCertificateControls()
    {
        certificateLabel = new Label( this, SWT.NONE );
        GridData certificateLabelData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        certificateLabelData.horizontalIndent = INPUT_INDENT * 2;
        certificateLabel.setLayoutData( certificateLabelData );
        certificateLabel.setText( Messages.urlInput_certificateFile );

        certificateText = new Text( this, SWT.BORDER );
        certificateText.setData( "name", "certificateText" );
        GridData certificateData = new GridData( SWT.FILL, SWT.CENTER, true, false, 2, 1 );
        certificateData.widthHint = 100;
        certificateText.setLayoutData( certificateData );
        certificateText.addModifyListener( modifyListener );

        browseCertificateButton = new Button( this, SWT.PUSH );
        browseCertificateButton.setData( "name", "browseCertificateButton" );
        browseCertificateButton.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        browseCertificateButton.setText( Messages.urlInput_browse );
        browseCertificateButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                FileDialog fd = new FileDialog( getShell(), SWT.OPEN );
                fd.setText( Messages.urlInput_fileSelect_title );
                String current = certificateText.getText().trim();
                fd.setFilterExtensions( new String[] { "*.p12;*.crt", "*.*" //$NON-NLS-1$ $NON-NLS-2$
                } );
                fd.setFilterNames( new String[] { Messages.urlInput_fileSelect_filter1,
                    Messages.urlInput_fileSelect_filter2 } );
                if ( current.length() > 0 )
                {
                    fd.setFileName( current );
                }
                String filename = fd.open();
                if ( filename != null )
                {
                    certificateText.setText( filename );
                }
            }
        } );

        passphraseLabel = new Label( this, SWT.NONE );
        GridData passphraseLabelData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        passphraseLabelData.horizontalIndent = INPUT_INDENT * 2;
        passphraseLabel.setLayoutData( passphraseLabelData );
        passphraseLabel.setText( Messages.urlInput_passphrase );

        passphraseText = new Text( this, SWT.BORDER | SWT.PASSWORD );
        passphraseText.setData( "name", "passphraseText" );
        GridData passphraseData = new GridData( SWT.LEFT, SWT.CENTER, false, false, 3, 1 );
        passphraseData.widthHint = INPUT_WIDTH;
        passphraseText.setLayoutData( passphraseData );
    }

    public String getUrlText()
    {
        return urlComponent.getText();
    }

    public String validate()
    {
        String url = getUrlText();

        if ( url.length() == 0 )
        {
            return Messages.urlInput_validation_enterUrl;
        }
        else
        {
            try
            {
                if ( !readonlyUrl )
                {
                    new URL( url );
                }
            }
            catch ( MalformedURLException e )
            {
                return NLS.bind( Messages.urlInput_validation_invalidUrl, e.getMessage() );
            }

            if ( !allowAnonymous && usernameText.getText().length() == 0 )
            {
                return Messages.urlInput_validation_enterUserCredentials;
            }
        }

        if ( displayCertificateControls && useCertificateButton.getSelection() )
        {
            String filename = certificateText.getText().trim();
            if ( filename.length() == 0 )
            {
                return Messages.urlInput_fileSelect_empty;
            }
            else
            {
                File file = new File( filename );
                if ( !file.exists() )
                {
                    return NLS.bind( Messages.urlInput_fileSelect_notFound, filename );
                }
            }
        }

        return null;
    }

    private void updateCredentials()
    {
        updating = true;

        IAuthData authData = AuthFacade.getAuthService().select( getUrlText() );
        if ( authData == null )
        {
            return;
        }

        if ( authData != null )
        {
            // TODO Disable username and password controls if anonymous is required
            if ( authData.allowsUsernameAndPassword() )
            {
                usernameText.setText( authData.getUsername() );
                passwordText.setText( authData.getPassword() );
            }

            if ( displayCertificateControls && authData.allowsCertificate() )
            {
                if ( !useCertificateButton.getSelection() )
                {
                    useCertificateButton.setSelection( true );
                    createCertificateControls();
                    getShell().pack();
                }

                if ( useCertificateButton.getSelection() )
                {
                    String certificateFilename = "";
                    String passphrase = "";
                    File file = authData.getCertificatePath();
                    if ( file != null )
                    {
                        certificateFilename = file.getAbsolutePath();
                    }
                    passphrase = authData.getCertificatePassphrase();
                    certificateText.setText( certificateFilename == null ? "" : certificateFilename );
                    passphraseText.setText( passphrase == null ? "" : passphrase );
                }
            }
        }
        else
        {
            // MECLIPSE-946 when no realm is found, just keep the previously entered/filled value..
            // setAnonymous( true );
            // usernameText.setText( "" );
            // passwordText.setText( "" );
        }

        updating = false;
    }

    public String getUrl()
    {
        inputHistory.save();
        saveAuthRealm();
        return getUrlText();
    }

    public void enableControls( boolean b )
    {
        urlComponent.setEnabled( b );
        usernameText.setEnabled( b );
        passwordText.setEnabled( b );
    }

    private void saveAuthRealm()
    {
        IAuthData authData = AuthFacade.getAuthService().select( getUrlText() );
        if ( authData == null )
        {
            authData = new AuthData();
        }
        if ( authData.allowsUsernameAndPassword() )
        {
            authData.setUsernameAndPassword( usernameText.getText(), passwordText.getText() );
        }
        if ( authData.allowsCertificate() )
        {
            File certificatePath = null;

            if ( displayCertificateControls && useCertificateButton.getSelection() )
            {
                String filename = certificateText.getText().trim();
                if ( filename.length() > 0 )
                {
                    certificatePath = new File( filename );
                }
            }
            authData.setSSLCertificate( certificatePath, passphraseText.getText() );
        }
        AuthFacade.getAuthService().save( getUrlText(), authData );
    }

    /**
     * @return the combo box component showing the url. can return null if the url is not represented by the combobox.
     */
    protected Combo getComboBoxComponent()
    {
        Control c = urlComponent.getWrappedControl();
        return c instanceof Combo ? (Combo) c : null;
    }

    public WidthGroup getWidthGroup()
    {
        return widthGroup;
    }

    private interface UrlFieldFacade
    {
        String getText();

        void setText( String text );

        void addModifyListener( ModifyListener listener );

        void addSelectionListener( SelectionListener listener );

        void setEnabled( boolean enabled );

        Control getWrappedControl();
    }
}
