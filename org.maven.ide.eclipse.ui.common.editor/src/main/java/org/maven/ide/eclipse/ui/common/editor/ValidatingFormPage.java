package org.maven.ide.eclipse.ui.common.editor;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.editor.internal.Activator;
import org.netbeans.validation.api.Problem;

abstract public class ValidatingFormPage
    extends FormPage
{
    private static final Image TOOLBAR_SPACER = Activator.getDefault().getImage( "toolbar_spacer.gif" );
    
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

    protected String toNull( String s )
    {
        return s.length() == 0 ? null : s;
    }

    protected String toNull( Text text )
    {
        return toNull( text.getText().trim() );
    }

    protected String toNull( CCombo combo )
    {
        return toNull( combo.getText().trim() );
    }

    protected void populateToolbar( final FormToolkit toolkit, ScrolledForm form )
    {
        FormEditor editor = getEditor();
        if ( editor instanceof AbstractFileEditor )
        {
            // The form header is created with a default toolbar item alignment,
            // which renders text labels below icons.
            // All this trouble is to create a new toolbar with the correct alignment.
            final ToolBarManager tbm = new ToolBarManager( SWT.FLAT | SWT.RIGHT );
            ( (AbstractFileEditor) editor ).contributeActions( tbm );

            ControlContribution cc = new ControlContribution( "toolbar" )
            {
                @Override
                protected Control createControl( Composite parent )
                {
                    if ( parent instanceof ToolBar )
                    {
                        // workaround for an ancient toolbar height bug
                        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=183003
                        ToolItem ti = new ToolItem( (ToolBar) parent, SWT.PUSH );
                        ti.setImage( TOOLBAR_SPACER );
                        ti.setDisabledImage( TOOLBAR_SPACER );
                        ti.setEnabled( false );
                        ti.setWidth( 0 );
                    }
                    Composite composite = toolkit.createComposite( parent );
                    composite.setBackground( null );
                    GridLayout layout = new GridLayout( 1, false );
                    layout.verticalSpacing = 0;
                    layout.marginHeight = 0;
                    layout.horizontalSpacing = 0;
                    layout.marginWidth = 0;
                    composite.setLayout( layout );
                    tbm.createControl( composite );
                    toolkit.paintBordersFor( composite );
                    return composite;
                }
            };

            // if not for the alignment, we only need two lines of code
            form.getToolBarManager().add( cc );
            form.updateToolBar();
        }
    }
}
