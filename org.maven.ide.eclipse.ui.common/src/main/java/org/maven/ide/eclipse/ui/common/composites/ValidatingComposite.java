package org.maven.ide.eclipse.ui.common.composites;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Validator;

public class ValidatingComposite
    extends Composite
{
    public static final int INPUT_INDENT = 10;

    private WidthGroup widthGroup;

    private SwtValidationGroup validationGroup;

    private boolean formMode;

    public ValidatingComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                boolean formMode )
    {
        super( parent, SWT.NONE );
        this.widthGroup = widthGroup == null ? new WidthGroup() : widthGroup;
        this.validationGroup = validationGroup;
        this.formMode = formMode;
    }

    protected int getButtonStyle()
    {
        return formMode ? SWT.FLAT : SWT.PUSH;
    }

    protected int getCComboStyle()
    {
        return formMode ? SWT.FLAT : SWT.BORDER;
    }

    protected boolean isFormMode()
    {
        return formMode;
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
}
