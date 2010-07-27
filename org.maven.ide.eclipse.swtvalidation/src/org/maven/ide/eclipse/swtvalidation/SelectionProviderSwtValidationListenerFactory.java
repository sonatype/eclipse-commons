package org.maven.ide.eclipse.swtvalidation;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationListenerFactory;
import org.netbeans.validation.api.ui.ValidationStrategy;
import org.netbeans.validation.api.ui.ValidationUI;

public class SelectionProviderSwtValidationListenerFactory
    extends ValidationListenerFactory<ISelectionProvider, ISelection>
{

    public SelectionProviderSwtValidationListenerFactory()
    {
        super( ISelectionProvider.class, ISelection.class );
    }

    @Override
    protected ValidationListener<ISelectionProvider> createListener( ISelectionProvider selectionProvider,
                                                                     ValidationStrategy vs, ValidationUI vui,
                                                                     Validator<ISelection> validators )
    {
        assert Display.getCurrent() != null : "Must be called on event thread";
        return new SelectionProviderSWTValidationListener( selectionProvider, vs, vui, validators );
    }

    private static class SelectionProviderSWTValidationListener
        extends ValidationListener<ISelectionProvider>
        implements ISelectionChangedListener
    {
        private Validator<ISelection> validator;

        private SelectionProviderSWTValidationListener( ISelectionProvider component, ValidationStrategy strategy,
                                                        ValidationUI validationUI, Validator<ISelection> validator )
        {
            super( ISelectionProvider.class, validationUI, component );
            this.validator = validator;
            if ( strategy == null )
            {
                throw new NullPointerException( "strategy null" );
            }

            switch ( strategy )
            {
                case DEFAULT:
                case ON_CHANGE_OR_ACTION:
                    component.addSelectionChangedListener( this );
                    break;
            }
            performValidation(); // Make sure any initial errors are discovered immediately.
        }

        @Override
        protected void performValidation( Problems problems )
        {
            ISelectionProvider component = getTarget();
            String name = null;
            if ( component instanceof Viewer )
            {
                Viewer viewer = (Viewer) component;
                Control control = viewer.getControl();
                if ( control.isDisposed() || !control.isEnabled() || viewer.getInput() == null )
                {
                    return;
                }
                name = SwtValidationGroup.nameForComponent( control );
            }
            validator.validate( problems, name, component.getSelection() );
        }

        public void selectionChanged( SelectionChangedEvent event )
        {
            performValidation();
        }
    }
}
