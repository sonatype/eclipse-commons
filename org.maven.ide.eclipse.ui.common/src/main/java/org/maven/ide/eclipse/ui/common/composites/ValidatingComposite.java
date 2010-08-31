package org.maven.ide.eclipse.ui.common.composites;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Validator;

public class ValidatingComposite
    extends Composite
{
    public static final int INPUT_INDENT = 10;

    private WidthGroup widthGroup;

    private SwtValidationGroup validationGroup;

    private FormToolkit toolkit;

    public ValidatingComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup )
    {
        this( parent, widthGroup, validationGroup, null );
    }

    public ValidatingComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                FormToolkit toolkit )
    {
        super( parent, SWT.NONE );
        this.widthGroup = widthGroup == null ? new WidthGroup() : widthGroup;
        this.validationGroup = validationGroup;
        this.toolkit = toolkit;

        if ( toolkit != null )
        {
            toolkit.adapt( this );
            toolkit.paintBordersFor( this );
        }
    }

    protected int getButtonStyle()
    {
        return toolkit == null ? SWT.PUSH : SWT.FLAT;
    }

    protected int getCComboStyle()
    {
        return toolkit == null ? SWT.BORDER : SWT.FLAT;
    }

    protected boolean isFormMode()
    {
        return toolkit != null;
    }

    protected void addToWidthGroup( Control control )
    {
        widthGroup.addControl( control );
    }

    protected GridData createInputData()
    {
        return createInputData( 1, 1 );
    }

    protected GridData createInputData( int horizontalSpan, int verticalSpan )
    {
        GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, horizontalSpan, verticalSpan );
        gd.widthHint = 100;
        gd.horizontalIndent = INPUT_INDENT;
        return gd;
    }

    @SuppressWarnings( "unchecked" )
    protected void addToValidationGroup( Control control, Validator<String> validator )
    {
        validationGroup.add( control, validator );
    }

    protected SwtValidationGroup getValidationGroup()
    {
        return validationGroup;
    }

    protected FormToolkit getToolkit()
    {
        return toolkit;
    }

    protected Label createLabel( String text )
    {
        Label label;
        if ( isFormMode() )
        {
            label = toolkit.createLabel( this, text );
        }
        else
        {
            label = new Label( this, SWT.NONE );
            label.setText( text );
        }

        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        return label;
    }

    protected Text createText( String controlName, String validationComponentName )
    {
        return createText( SWT.NONE, 1, 1, controlName, validationComponentName );
    }

    protected Text createText( int style, int horizontalSpan, int verticalSpan, String controlName,
                               String validationComponentName )
    {
        Text text;
        if ( isFormMode() )
        {
            text = toolkit.createText( this, "", style ); //$NON-NLS-1$
        }
        else
        {
            text = new Text( this, SWT.BORDER | style );
        }

        text.setLayoutData( createInputData( horizontalSpan, verticalSpan ) );
        if ( controlName != null )
        {
            text.setData( "name", controlName ); //$NON-NLS-1$
        }
        if ( validationComponentName != null )
        {
            SwtValidationGroup.setComponentName( text, validationComponentName );
        }

        return text;
    }

    protected Button createButton( String text )
    {
        Button button;
        if ( isFormMode() )
        {
            button = toolkit.createButton( this, text, getButtonStyle() );
        }
        else
        {
            button = new Button( this, getButtonStyle() );
            button.setText( text );
        }
        button.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, false, false ) );

        return button;
    }

    protected Button createCheckbox( String text, int horizontalSpan, int verticalSpan, String controlName )
    {
        Button checkbox;
        if ( isFormMode() )
        {
            checkbox = toolkit.createButton( this, text, SWT.CHECK );
        }
        else
        {
            checkbox = new Button( this, SWT.CHECK );
            checkbox.setText( text );
        }
        GridData data = new GridData( SWT.LEFT, SWT.CENTER, false, false, horizontalSpan, verticalSpan );
        data.horizontalIndent = INPUT_INDENT;
        checkbox.setLayoutData( data );

        if ( controlName != null )
        {
            checkbox.setData( "name", controlName ); //$NON-NLS-1$
        }

        return checkbox;
    }
}
