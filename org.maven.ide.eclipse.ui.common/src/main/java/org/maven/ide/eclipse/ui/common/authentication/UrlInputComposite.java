package org.maven.ide.eclipse.ui.common.authentication;

import java.beans.Beans;
import java.io.File;

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
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.InputHistory;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.composites.ValidatingComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

/**
 * Reusable widget to enter a URL and/or supply the associated credentials.
 * <P/>
 * When the widget is initially displayed, it checks if the auth registry already contains credentials for the given
 * URL, and populates the username/password and certificate fields. The URL can be rendered read-only or editable, a
 * drop-down input history is provided.
 * <P/>
 * The component is ready to be used in dialogs and wizard pages.
 * <P/>
 * Once the wizard flow is complete, call the getUrl() method to retrieve the URL. The credentials will be automatically
 * saved in the auth registry.
 */
public class UrlInputComposite
    extends ValidatingComposite
{
    public static final String URL_CONTROL_NAME = "urlControl";

    public static final String ANONYMOUS_LABEL_NAME = "anonymousLabel";

    public static final String USERNAME_TEXT_NAME = "usernameText";

    public static final String PASSWORD_TEXT_NAME = "passwordText";

    public static final String CERTIFICATE_TEXT_NAME = "certificateText";

    public static final String PASSPHRASE_TEXT_NAME = "passphraseText";

    public static final String BROWSE_CERTIFICATE_BUTTON_NAME = "browseCertificateButton";

    public static final String USE_CERTIFICATE_BUTTON_NAME = "useCertificateButton";

    public static final int READ_ONLY_URL = 1;

    public static final int ALLOW_ANONYMOUS = 2;

    public static final int CERTIFICATE_CONTROLS = 4;

    private static final int INPUT_WIDTH = 200;

    private static final int INPUT_INDENT = 10;

    private static final String SETTINGS = "urls";

    private UrlFieldFacade urlComponent;

    private Label urlLabel;

    private Text usernameText;

    private Text passwordText;

    protected boolean updating = false;

    private boolean readonlyUrl = true;

    private InputHistory inputHistory;

    private String urlLabelText;

    private Label usernameLabel;

    private Label passwordLabel;

    private Label certificateLabel;

    private Label anonymousLabel;

    private Text certificateText;

    private Button browseCertificateButton;

    private Label passphraseLabel;

    private Text passphraseText;

    private IAuthData authData;

    private IAuthData defaultAuthData;

    private boolean initialized;

    private String url;

    private String username;

    private String password;

    private String passphrase;

    private String certificate;

    private boolean dirty;

    public UrlInputComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup, int style )
    {
        super( parent, widthGroup, validationGroup );
        this.readonlyUrl = ( style & READ_ONLY_URL ) != 0;
        boolean allowAnonymous = ( style & ALLOW_ANONYMOUS ) != 0;
        this.urlLabelText = Messages.urlInput_url_label;

        AnonymousAccessType anonymousAccessType =
            allowAnonymous ? AnonymousAccessType.ALLOWED : AnonymousAccessType.NOT_ALLOWED;
        if ( ( style & CERTIFICATE_CONTROLS ) != 0 )
        {
            defaultAuthData = new AuthData( "", "", null, null, anonymousAccessType );
        }
        else
        {
            defaultAuthData = new AuthData( "", "", anonymousAccessType );
        }
        authData = defaultAuthData;

        setInputHistory( new InputHistory( SETTINGS ) );

        setLayout( new GridLayout( 4, false ) );

        createControls();

        urlComponent.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                url = urlComponent.getText();
                setDirty();
                updateCredentials();
            }
        } );
        urlComponent.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                url = urlComponent.getText();
                setDirty();
                updateCredentials();
            }
        } );
        SwtValidationGroup.setComponentName( urlComponent.getWrappedControl(), Messages.urlInput_url_name );
        addToValidationGroup( urlComponent.getWrappedControl(), new Validator<String>()
        {
            public Class<String> modelType()
            {
                return String.class;
            }

            public void validate( Problems problems, String compName, String model )
            {
                SonatypeValidators.URL_MUST_BE_VALID.validate( problems, compName, model );
            }
        } );

        updateCredentials();
        initialized = true;
    }

    public class CredentialsValidator
        implements Validator<String>
    {
        public Class<String> modelType()
        {
            return String.class;
        }

        public void validate( Problems problems, String compName, String model )
        {
            if ( AnonymousAccessType.NOT_ALLOWED.equals( authData.getAnonymousAccessType() ) )
            {
                StringValidators.REQUIRE_NON_EMPTY_STRING.validate( problems, compName, model );
            }
        }
    };

    public void setInputHistory( InputHistory hist )
    {
        assert hist != null;
        inputHistory = hist;
        if ( urlComponent != null )
        {
            // setting different input history after the ui component hierarchy was constructed
            Control c = urlComponent.getWrappedControl();
            if ( c instanceof Combo )
            {
                inputHistory.add( urlLabelText, (Combo) c );
                inputHistory.load();
            }
        }
    }

    public void setUrlLabelText( String text )
    {
        urlLabelText = text;
        if ( urlLabel != null && !urlLabel.isDisposed() )
        {
            urlLabel.setText( text );
        }
        setInputHistory( new InputHistory( SETTINGS ) );
    }

    protected void createControls()
    {
        urlLabel = new Label( this, SWT.NONE );
        urlLabel.setText( urlLabelText );
        urlLabel.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( urlLabel );

        createUrlControl();
    }

    protected void createUrlControl()
    {
        UrlFieldFacade facade;
        if ( readonlyUrl )
        {
            final Text urlText = new Text( this, SWT.READ_ONLY );
            urlText.setData( "name", URL_CONTROL_NAME );
            GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 );
            gd.widthHint = 100;
            gd.horizontalIndent = INPUT_INDENT;
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
        else
        {
            final Combo combo = new Combo( this, SWT.NONE );
            combo.setData( "name", URL_CONTROL_NAME );
            GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 );
            gd.widthHint = 100;
            gd.horizontalIndent = INPUT_INDENT;
            combo.setLayoutData( gd );

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

    boolean hasUsernamePasswordControls = false;

    /**
     * Creates the controls for username and password authentication.
     * 
     * @return true if the layout has changed
     */
    private boolean createUsernamePasswordControls()
    {
        if ( hasUsernamePasswordControls )
        {
            return false;
        }
        hasUsernamePasswordControls = true;

        usernameLabel = new Label( this, SWT.NONE );
        usernameLabel.setText( Messages.urlInput_username_label );
        GridData usernameLabelData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        usernameLabelData.horizontalIndent = INPUT_INDENT;
        usernameLabel.setLayoutData( usernameLabelData );
        addToWidthGroup( usernameLabel );

        usernameText = new Text( this, SWT.BORDER );
        usernameText.setData( "name", USERNAME_TEXT_NAME );
        GridData usernameGridData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        usernameGridData.widthHint = INPUT_WIDTH;
        usernameGridData.horizontalIndent = INPUT_INDENT;
        usernameText.setLayoutData( usernameGridData );
        usernameText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                username = usernameText.getText();
                setDirty();
            }
        } );
        // Set focus to username if the URL is read-only
        if ( readonlyUrl )
            usernameText.setFocus();

        anonymousLabel = new Label( this, SWT.NONE );
        anonymousLabel.setText( Messages.urlInput_anonymousIfEmpty );
        anonymousLabel.setData( "name", ANONYMOUS_LABEL_NAME );
        anonymousLabel.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 2, 1 ) );

        passwordLabel = new Label( this, SWT.NONE );
        passwordLabel.setText( Messages.urlInput_password_label );
        GridData passwordLabelData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        passwordLabelData.horizontalIndent = INPUT_INDENT;
        passwordLabel.setLayoutData( passwordLabelData );
        addToWidthGroup( passwordLabel );

        passwordText = new Text( this, SWT.BORDER | SWT.PASSWORD );
        passwordText.setData( "name", PASSWORD_TEXT_NAME );
        GridData passwordGridData = new GridData( SWT.LEFT, SWT.CENTER, false, false, 3, 1 );
        passwordGridData.widthHint = INPUT_WIDTH;
        passwordGridData.horizontalIndent = INPUT_INDENT;
        passwordText.setLayoutData( passwordGridData );
        passwordText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                password = passwordText.getText();
                setDirty();
            }
        } );

        SwtValidationGroup.setComponentName( usernameText, Messages.urlInput_username_name );
        SwtValidationGroup.setComponentName( passwordText, Messages.urlInput_password_name );
        addToValidationGroup( usernameText, new CredentialsValidator() );
        // addToValidationGroup( passwordText, new CredentialsValidator() );

        return true;
    }

    /**
     * Removes the controls for username and password authentication.
     * 
     * @return true if the layout has changed
     */
    private boolean removeUsernamePasswordControls()
    {
        if ( !hasUsernamePasswordControls )
        {
            return false;
        }
        hasUsernamePasswordControls = false;

        usernameLabel.dispose();
        usernameText.dispose();
        anonymousLabel.dispose();
        passwordLabel.dispose();
        passwordText.dispose();

        return true;
    }

    boolean hasCertificateControls = false;

    /**
     * Removes the controls for SSL certificate authentication.
     * 
     * @return true if the layout has changed
     */
    private boolean removeCertificateControls()
    {
        if ( !hasCertificateControls )
        {
            return false;
        }
        hasCertificateControls = false;

        certificateLabel.dispose();
        certificateText.dispose();
        browseCertificateButton.dispose();
        passphraseLabel.dispose();
        passphraseText.dispose();

        return true;
    }

    /**
     * Creates the controls for SSL certificate authentication.
     * 
     * @return true if the layout has changed
     */
    private boolean createCertificateControls()
    {
        if ( hasCertificateControls )
        {
            return false;
        }
        hasCertificateControls = true;

        certificateLabel = new Label( this, SWT.NONE );
        GridData certificateLabelData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        certificateLabelData.horizontalIndent = INPUT_INDENT;
        certificateLabel.setLayoutData( certificateLabelData );
        certificateLabel.setText( Messages.urlInput_certificateFile_label );
        addToWidthGroup( certificateLabel );

        certificateText = new Text( this, SWT.BORDER );
        certificateText.setData( "name", CERTIFICATE_TEXT_NAME );
        GridData certificateData = new GridData( SWT.FILL, SWT.CENTER, true, false, 2, 1 );
        certificateData.widthHint = 100;
        certificateData.horizontalIndent = INPUT_INDENT;
        certificateText.setLayoutData( certificateData );
        SwtValidationGroup.setComponentName( certificateText, Messages.urlInput_certificateFile_name );
        addToValidationGroup( certificateText, StringValidators.REQUIRE_NON_EMPTY_STRING );
        certificateText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                certificate = certificateText.getText();
                setDirty();
            }
        } );

        browseCertificateButton = new Button( this, SWT.PUSH );
        browseCertificateButton.setData( "name", BROWSE_CERTIFICATE_BUTTON_NAME );
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
        passphraseLabelData.horizontalIndent = INPUT_INDENT;
        passphraseLabel.setLayoutData( passphraseLabelData );
        passphraseLabel.setText( Messages.urlInput_passphrase_label );
        addToWidthGroup( passphraseLabel );

        passphraseText = new Text( this, SWT.BORDER | SWT.PASSWORD );
        passphraseText.setData( "name", PASSPHRASE_TEXT_NAME );
        GridData passphraseData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
        passphraseData.widthHint = INPUT_WIDTH;
        passphraseData.horizontalIndent = INPUT_INDENT;
        passphraseText.setLayoutData( passphraseData );
        SwtValidationGroup.setComponentName( passphraseText, Messages.urlInput_passphrase_name );
        // addToValidationGroup( passphraseText, StringValidators.REQUIRE_NON_EMPTY_STRING );
        passphraseText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                passphrase = passphraseText.getText();
                setDirty();
            }
        } );

        return true;
    }

    public String getUrlText()
    {
        return url == null ? "" : url;
    }

    public void updateCredentials()
    {
        if ( Beans.isDesignTime() )
        {
            return;
        }

        updating = true;

        IAuthData newAuthData = AuthFacade.getAuthService().select( getUrlText() );
        if ( newAuthData == null )
        {
            authData = defaultAuthData;
        }
        else
        {
            authData = newAuthData;
        }

        boolean usernamePasswordLayoutHasChanged = false;
        if ( authData.allowsUsernameAndPassword() )
        {
            usernamePasswordLayoutHasChanged = createUsernamePasswordControls();
            if ( AnonymousAccessType.REQUIRED.equals( authData.getAnonymousAccessType() ) )
            {
                authData.setUsernameAndPassword( "", "" );
                usernameText.setEnabled( false );
                passwordText.setEnabled( false );
            }
            else
            {
                usernameText.setEnabled( true );
                passwordText.setEnabled( true );
            }
            usernameText.setText( authData.getUsername() );
            passwordText.setText( authData.getPassword() );
            anonymousLabel.setVisible( authData.allowsAnonymousAccess() );
        }
        else
        {
            usernamePasswordLayoutHasChanged = removeUsernamePasswordControls();
        }


        boolean certificateLayoutHasChanged = false;
        if ( authData.allowsCertificate() )
        {
            certificate = null;
            passphrase = null;
            File file = authData.getCertificatePath();
            if ( file != null )
            {
                certificate = file.getAbsolutePath();
            }
            passphrase = authData.getCertificatePassphrase();

            certificateLayoutHasChanged = createCertificateControls();
            certificateText.setText( certificate == null ? "" : certificate );
            passphraseText.setText( passphrase == null ? "" : passphrase );
        }
        else
        {
            certificateLayoutHasChanged = removeCertificateControls();
        }

        updating = false;

        if ( initialized && ( usernamePasswordLayoutHasChanged || certificateLayoutHasChanged ) )
        {
            getShell().pack();
        }
        if ( getValidationGroup() != null )
        {
            getValidationGroup().performValidation();
        }
    }

    public String getUrl()
    {
        if ( dirty )
        {
            inputHistory.save();
            saveCredentials();
            dirty = false;
        }
        return getUrlText();
    }

    public void setUrl( String text )
    {
        if ( text != null )
        {
            urlComponent.setText( text );
            url = text;
            updateCredentials();
        }
    }

    public void enableControls( boolean b )
    {
        urlComponent.setEnabled( b );
    }

    private void setDirty()
    {
        if ( !updating )
        {
            dirty = true;
        }
    }

    private void saveCredentials()
    {
        if ( Beans.isDesignTime() )
        {
            return;
        }
        Problem validationProblem = getValidationGroup().performValidation();
        if (validationProblem != null && validationProblem.isFatal())
        {
            return;
        }
        
        if ( authData.allowsUsernameAndPassword() )
        {
            authData.setUsernameAndPassword( username, password );
        }
        if ( authData.allowsCertificate() )
        {
            File certificatePath = null;

            String filename = certificate;
            if ( filename.length() > 0 )
            {
                certificatePath = new File( filename );
            }
            authData.setSSLCertificate( certificatePath, passphrase );
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

    private interface UrlFieldFacade
    {
        String getText();

        void setText( String text );

        void addModifyListener( ModifyListener listener );

        void addSelectionListener( SelectionListener listener );

        void setEnabled( boolean enabled );

        Control getWrappedControl();
    }

    public void addModifyListener( ModifyListener modifyListener )
    {
        urlComponent.addModifyListener( modifyListener );
    }
}
