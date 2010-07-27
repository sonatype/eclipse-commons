package org.maven.ide.eclipse.ui.common.composites;

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;
import static org.maven.ide.eclipse.ui.common.FormUtils.toNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;

public class GAVComposite
    extends ValidatingComposite
{
    private Text groupIdText;

    private Text artifactIdText;

    private Text versionText;

    public GAVComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup, boolean formMode )
    {
        super( parent, widthGroup, validationGroup, formMode );

        setLayout( new GridLayout( 2, false ) );

        createGroupIdControls();
        createArtifactIdControls();
        createVersionControls();
    }

    private void createGroupIdControls()
    {
        Label groupIdLabel = new Label( this, SWT.NONE );
        groupIdLabel.setText( Messages.gavComposite_groupId_label );
        groupIdLabel.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( groupIdLabel );

        groupIdText = new Text( this, SWT.BORDER );
        groupIdText.setLayoutData( createInputData() );
        groupIdText.setData( "name", "groupIdText" ); //$NON-NLS-1$ //$NON-NLS-2$
        groupIdText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveGroupId( getGroupId() );
            }
        } );
        SwtValidationGroup.setComponentName( groupIdText, Messages.gavComposite_groupId_name );
        addToValidationGroup( groupIdText, SonatypeValidators.createGroupIdValidators() );
    }

    @SuppressWarnings( "unchecked" )
    private void createArtifactIdControls()
    {
        Label artifactIdLabel = new Label( this, SWT.NONE );
        artifactIdLabel.setText( Messages.gavComposite_artifactId_label );
        artifactIdLabel.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( artifactIdLabel );

        artifactIdText = new Text( this, SWT.BORDER );
        artifactIdText.setLayoutData( createInputData() );
        artifactIdText.setData( "name", "artifactIdText" ); //$NON-NLS-1$ //$NON-NLS-2$
        artifactIdText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveArtifactId( getArtifactId() );
            }
        } );
        SwtValidationGroup.setComponentName( artifactIdText, Messages.gavComposite_artifactId_name );

        Validator<String> validator = SonatypeValidators.createArtifactIdValidators();
        addToValidationGroup( artifactIdText, isFormMode() ? validator
                        : ValidatorUtils.merge( validator, SonatypeValidators.EXISTS_IN_WORKSPACE ) );
    }

    private void createVersionControls()
    {
        Label versionLabel = new Label( this, SWT.NONE );
        versionLabel.setText( Messages.gavComposite_version_label );
        versionLabel.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( versionLabel );

        versionText = new Text( this, SWT.BORDER );
        versionText.setLayoutData( createInputData() );
        versionText.setData( "name", "versionText" ); //$NON-NLS-1$ //$NON-NLS-2$
        versionText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveVersion( getVersion() );
            }
        } );
        SwtValidationGroup.setComponentName( versionText, Messages.gavComposite_version_name );
        addToValidationGroup( versionText, SonatypeValidators.createVersionValidators() );
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
}
