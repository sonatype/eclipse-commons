package org.maven.ide.eclipse.ui.common.composites;

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;
import static org.maven.ide.eclipse.ui.common.FormUtils.toNull;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.osgi.framework.Version;

public class GAVComposite
    extends ValidatingComposite
{
    public static final int VALIDATE_PROJECT_NAME = 1;

    public static final int VALIDATE_OSGI_VERSION = 2;

    private Text groupIdText;

    private Text artifactIdText;

    private Text versionText;

    public GAVComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                         FormToolkit toolkit, int style )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        setLayout( new GridLayout( 2, false ) );

        createGroupIdControls();
        createArtifactIdControls( ( style & VALIDATE_PROJECT_NAME ) == VALIDATE_PROJECT_NAME );
        createVersionControls( ( style & VALIDATE_OSGI_VERSION ) == VALIDATE_OSGI_VERSION );
    }

    private void createGroupIdControls()
    {
        createLabel( Messages.gavComposite_groupId_label );

        groupIdText = createText( "groupIdText", Messages.gavComposite_groupId_name ); //$NON-NLS-1$ 
        groupIdText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveGroupId( getGroupId() );
            }
        } );
        addToValidationGroup( groupIdText, SonatypeValidators.createGroupIdValidators() );
    }

    @SuppressWarnings( "unchecked" )
    private void createArtifactIdControls( boolean validateProjectName )
    {
        createLabel( Messages.gavComposite_artifactId_label );

        artifactIdText = createText( "artifactIdText", Messages.gavComposite_artifactId_name ); //$NON-NLS-1$
        artifactIdText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveArtifactId( getArtifactId() );
            }
        } );

        Validator<String> validator = SonatypeValidators.createArtifactIdValidators();
        addToValidationGroup( artifactIdText,
                              validateProjectName ? ValidatorUtils.merge( validator,
                                                                          SonatypeValidators.EXISTS_IN_WORKSPACE )
                                              : validator );
    }

    @SuppressWarnings( "unchecked" )
    private void createVersionControls( boolean validateOsgiVersion )
    {
        createLabel( Messages.gavComposite_version_label );

        versionText = createText( "versionText", Messages.gavComposite_version_name ); //$NON-NLS-1$
        versionText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveVersion( getVersion() );
            }
        } );
        addToValidationGroup( versionText,
                              validateOsgiVersion ? //
                              ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING,
                                                    StringValidators.NO_WHITESPACE, new Validator<String>()
                                                    {
                                                        public void validate( Problems problems, String compName,
                                                                              String model )
                                                        {
                                                            try
                                                            {
                                                                new Version( model );
                                                            }
                                                            catch ( IllegalArgumentException e )
                                                            {
                                                                problems.add( NLS.bind( Messages.gavComposite_invalidOsgiVersion,
                                                                                        compName ) );
                                                            }
                                                        }

                                                        public Class<String> modelType()
                                                        {
                                                            return String.class;
                                                        }
                                                    } )
                                              : SonatypeValidators.createVersionValidators() );
    }

    @Override
    protected GridData createInputData()
    {
        GridData gd = super.createInputData();
        if ( !isFormMode() )
        {
            gd.horizontalAlignment = SWT.LEFT;
            gd.grabExcessHorizontalSpace = false;
            gd.widthHint = 200;
        }
        return gd;
    }

    public void setGroupId( String groupId )
    {
        groupIdText.setText( nvl( groupId ) );
    }

    public void setArtifactId( String artifactId )
    {
        artifactIdText.setText( nvl( artifactId ) );
    }

    public void setVersion( String version )
    {
        versionText.setText( nvl( version ) );
    }

    public String getGroupId()
    {
        return toNull( groupIdText.getText() );
    }

    public String getArtifactId()
    {
        return toNull( artifactIdText.getText() );
    }

    public String getVersion()
    {
        return toNull( versionText.getText() );
    }

    protected void saveGroupId( String groupId )
    {
    }

    protected void saveArtifactId( String artifactId )
    {
    }

    protected void saveVersion( String version )
    {
    }

    public void setEditable( boolean b )
    {
        groupIdText.setEditable( b );
        artifactIdText.setEditable( b );
        versionText.setEditable( b );
    }
}
