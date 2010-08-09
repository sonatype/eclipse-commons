package org.maven.ide.eclipse.ui.common.test;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;

public abstract class AbstractWizardPageTest
    extends TestCase
{
    protected static final IProgressMonitor monitor = new NullProgressMonitor();

    protected void setText( WizardPage page, String name, String content )
    {
        Control control = getControlByName( page, name );
        Text text = (Text) control;
        text.setText( content );
    }

    protected void assertText( WizardPage page, String name, String content, boolean isEnabled )
    {
        Control control = getControlByName( page, name );
        Text text = (Text) control;
        Assert.assertEquals( content, text.getText() );
        Assert.assertEquals( isEnabled, text.getEditable() );
    }

    protected void assertCombo( WizardPage page, String name, String content, boolean isEnabled )
    {
        Control control = getControlByName( page, name );
        Combo combo = (Combo) control;
        Assert.assertEquals( content, combo.getText() );
        Assert.assertEquals( isEnabled, combo.isEnabled() );
    }

    protected void assertButton( WizardPage page, String name, boolean isEnabled )
    {
        Control control = getControlByName( page, name );
        Button text = (Button) control;
        Assert.assertEquals( isEnabled, text.isEnabled() );
    }

    protected Control getControlByName( WizardPage page, String name )
    {
        Control result = getControlByName( (Composite) ( page.getControl() ), name );
        assertNotNull( "Cannot find control with name=" + name, result );
        return result;
    }

    protected Control getControlByName( Composite composite, String name )
    {
        for ( Control control : composite.getChildren() )
        {
            Object controlName = control.getData( "name" );
            System.out.println( control.getClass().getCanonicalName() + ": Name: " + controlName );
            if ( name.equals( control.getData( "name" ) ) )
            {
                return control;
            }
            if ( control instanceof Composite )
            {
                Control found = getControlByName( (Composite) control, name );
                if ( found != null )
                {
                    return found;
                }
            }
        }
        return null;
    }
}
