package org.maven.ide.eclipse.ui.common.editor;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.netbeans.validation.api.Problem;

abstract public class ValidatingFormPage
    extends FormPage
{
    private boolean dirty;

    private boolean updating;

    private SwtValidationGroup validationGroup;

    public ValidatingFormPage( FormEditor editor, String id, String title )
    {
        super( editor, id, title );

        validationGroup = SwtValidationGroup.create( SwtValidationUI.createFormUI( this ) );
    }

    protected SwtValidationGroup getValidationGroup()
    {
        return validationGroup;
    }

    abstract protected void update();

    protected String validate()
    {
        Problem problem = validationGroup.performValidation();
        if ( problem != null && problem.isFatal() )
        {
            return problem.getMessage();
        }
        return null;
    }

    @Override
    public boolean isDirty()
    {
        return dirty;
    }

    public void setDirty( boolean b )
    {
        if ( !updating )
        {
            dirty = b;
            firePropertyChange( PROP_DIRTY );

            validatePage();
        }
    }

    public void validatePage()
    {
        IManagedForm managedForm = getManagedForm();
        if ( managedForm != null )
        {
            validate();
        }
    }

    public boolean hasErrors()
    {
        return validate() != null;
    }

    public void updatePage()
    {
        updating = true;
        update();
        validatePage();
        updating = false;
    }

    protected boolean isUpdating()
    {
        return updating;
    }
}
