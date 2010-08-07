package org.maven.ide.eclipse.ui.common.authentication;

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.IAuthRegistry;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.composites.ValidatingComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

public class RealmManagementComposite
    extends ValidatingComposite
{
    private Text idText;

    private String id;

    private Text nameText;

    private String name;

    private Text descriptionText;

    private String description;

    private Combo authenticationCombo;

    private AuthenticationType authenticationType;

    private boolean updating;

    private boolean dirty;

    private boolean newRealm;

    private AuthenticationType[] authenticationOptions =
        new AuthenticationType[] { AuthenticationType.USERNAME_PASSWORD, AuthenticationType.CERTIFICATE,
            AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD };

    private String[] authenticationLabels =
        new String[] { Messages.realmManagementComposite_authenticationType_password, Messages.realmManagementComposite_authenticationType_ssl, Messages.realmManagementComposite_authenticationType_passwordAndSsl };

    public RealmManagementComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup )
    {
        super( parent, widthGroup, validationGroup, false );
        setLayout( new GridLayout( 3, false ) );

        createIdControls();
        createNameControls();
        createDescriptionControls();
        createAuthenticationControls();
    }

    private void createIdControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_realmId_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        idText = new Text( this, SWT.BORDER );
        idText.setLayoutData( createInputData( 2, 1 ) );
        idText.setData( "name", "idText" ); //$NON-NLS-1$ //$NON-NLS-2$
        idText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                id = idText.getText();
                setDirty();
            }
        } );
        SwtValidationGroup.setComponentName( idText, Messages.realmManagementComposite_realmId_name );
        addToValidationGroup( idText, new Validator<String>()
        {
            public void validate( Problems problems, String componentName, String model )
            {
                if ( model.length() > 0 && AuthFacade.getAuthRegistry().getRealm( model ) != null )
                {
                    problems.add( NLS.bind( Messages.realmManagementComposite_realmId_exists, model ) );
                }
                else
                {
                    StringValidators.REQUIRE_NON_EMPTY_STRING.validate( problems, componentName, model );
                }
            }

            public Class<String> modelType()
            {
                return String.class;
            }
        } );
    }

    private void createNameControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_realmName_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        nameText = new Text( this, SWT.BORDER );
        nameText.setLayoutData( createInputData( 2, 1 ) );
        nameText.setData( "name", "nameText" ); //$NON-NLS-1$ //$NON-NLS-2$
        nameText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                name = nameText.getText();
                setDirty();
            }
        } );
        SwtValidationGroup.setComponentName( nameText, Messages.realmManagementComposite_realmName_name );
        addToValidationGroup( nameText, StringValidators.REQUIRE_NON_EMPTY_STRING );
    }

    private void createDescriptionControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_realmDescription_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        descriptionText = new Text( this, SWT.BORDER );
        descriptionText.setLayoutData( createInputData( 2, 1 ) );
        descriptionText.setData( "name", "descriptionText" ); //$NON-NLS-1$ //$NON-NLS-2$
        descriptionText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                description = descriptionText.getText();
                setDirty();
            }
        } );
    }

    private void createAuthenticationControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_realmAuthentication_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        authenticationCombo = new Combo( this, SWT.READ_ONLY );
        authenticationCombo.setItems( authenticationLabels );
        authenticationCombo.select( 0 );
        authenticationCombo.setData( "name", "authenticationCombo" ); //$NON-NLS-1$ //$NON-NLS-2$
        authenticationCombo.setLayoutData( createInputData( 2, 1 ) );
        authenticationCombo.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                int n = authenticationCombo.getSelectionIndex();
                if ( n >= 0 )
                {
                    authenticationType = authenticationOptions[n];
                    setDirty();
                }
            }
        } );
    }

    public void setControlsEnabled( boolean enabled )
    {
        idText.setEnabled( enabled );
        nameText.setEnabled( enabled );
        descriptionText.setEnabled( enabled );
        authenticationCombo.setEnabled( enabled );
    }

    public void setRealm( IAuthRealm realm )
    {
        dirty = false;
        updating = true;

        setControlsEnabled( true );

        if ( realm == null )
        {
            newRealm = true;

            idText.setText( "" ); //$NON-NLS-1$
            nameText.setText( "" ); //$NON-NLS-1$
            descriptionText.setText( "" ); //$NON-NLS-1$
            authenticationCombo.select( 0 );
        }
        else
        {
            newRealm = false;
            idText.setEnabled( false );

            idText.setText( nvl( realm.getId() ) );
            nameText.setText( nvl( realm.getName() ) );
            descriptionText.setText( nvl( realm.getDescription() ) );

            authenticationType = realm.getAuthenticationType();
            int selection = 0;
            for ( int i = authenticationOptions.length - 1; i >= 0; i-- )
            {
                if ( authenticationOptions[i].equals( authenticationType ) )
                {
                    selection = i;
                    break;
                }
            }
            authenticationCombo.select( selection );
        }

        updating = false;
    }

    public void setDirty()
    {
        if ( !updating )
        {
            dirty = true;
        }
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public String save( IProgressMonitor monitor )
    {
        if ( dirty )
        {
            IAuthRegistry authRegistry = AuthFacade.getAuthRegistry();
            IAuthRealm authRealm = null;
            if ( !newRealm )
            {
                authRealm = authRegistry.getRealm( id );
            }

            if ( authRealm == null )
            {
                authRegistry.addRealm( id, name, description, authenticationType, monitor );
            }
            else
            {
                authRealm.setName( name );
                authRealm.setDescription( description );
                authRealm.setAuthenticationType( authenticationType );
                authRegistry.updateRealm( authRealm, monitor );
            }

            dirty = false;

            return id;
        }
        return null;
    }
}
