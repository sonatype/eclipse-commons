package org.maven.ide.eclipse.ui.tests.common;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;

public abstract class AbstractWizardPageTest
    extends TestCase
{
    protected Wizard wizard;

    protected static final IProgressMonitor monitor = new NullProgressMonitor();

    protected void setText( DialogPage page, String name, String content )
    {
        Control control = getControlByName( page, name );
        Text text = (Text) control;
        text.setText( content );
    }

    protected void assertText( DialogPage page, String name, String content, boolean isEnabled, boolean isVisible )
    {
        Control control = getControlByName( page, name, false /* assertExists */);
        if ( !isVisible && control == null )
        {
            return;
        }
        assertNotNull( "Cannot find control with name=" + name, control );
        Text text = (Text) control;
        if ( isVisible )
        {
            Assert.assertEquals( "Incorrect expected value for control " + name, content, text.getText() );
        }
        Assert.assertEquals( "Incorrect enabled status for control " + name, isEnabled,
                             text.getEnabled() && text.getEditable() );
        Assert.assertEquals( "Incorrect visible status for control " + name, isVisible, text.getVisible() );
    }

    protected void assertLabel( DialogPage page, String name, boolean isVisible )
    {
        Control control = getControlByName( page, name, false /* assertExists */);
        if ( !isVisible && control == null )
        {
            return;
        }
        assertNotNull( "Cannot find control with name=" + name, control );
        Label label = (Label) control;
        Assert.assertEquals( "Incorrect visible status for control " + name, isVisible, label.getVisible() );
    }

    protected void assertCombo( DialogPage page, String name, String content, boolean isEnabled )
    {
        Control control = getControlByName( page, name );
        Combo combo = (Combo) control;
        Assert.assertEquals( "Incorrect expected value for control " + name, content, combo.getText() );
        Assert.assertEquals( "Incorrect enabled status for control " + name, isEnabled, combo.isEnabled() );
    }

    protected void assertButton( DialogPage page, String name, boolean isEnabled, boolean isVisible )
    {
        Control control = getControlByName( page, name, false /* assertExists */);
        if ( !isVisible && control == null )
        {
            return;
        }
        assertNotNull( "Cannot find control with name=" + name, control );
        Button button = (Button) control;
        Assert.assertEquals( "Incorrect enabled status for control " + name, isEnabled, button.getEnabled() );
        Assert.assertEquals( "Incorrect visible status for control " + name, isVisible, button.getVisible() );
    }

    protected void assertCheckbox( DialogPage page, String name, boolean isChecked, boolean isEnabled, boolean isVisible )
    {
        Control control = getControlByName( page, name, false /* assertExists */);
        if ( !isVisible && control == null )
        {
            return;
        }
        assertNotNull( "Cannot find control with name=" + name, control );
        Button button = (Button) control;
        Assert.assertEquals( "Incorrect expected value for control " + name, isChecked, button.getSelection() );
        Assert.assertEquals( "Incorrect enabled status for control " + name, isEnabled, button.getEnabled() );
        Assert.assertEquals( "Incorrect visible status for control " + name, isVisible, button.getVisible() );
    }

    protected Control getControlByName( DialogPage page, String name )
    {
        return getControlByName( page, name, true /* assertExists */);
    }

    protected Control getControlByName( DialogPage page, String name, boolean assertExists )
    {
        Control result = getControlByName( (Composite) ( page.getControl() ), name );
        if ( assertExists )
        {
            assertNotNull( "Cannot find control with name=" + name, result );
        }
        return result;
    }

    protected Control getControlByName( Composite composite, String name )
    {
        for ( Control control : composite.getChildren() )
        {
            Object controlName = control.getData( "name" );
            System.out.println( control.getClass().getCanonicalName() + ": Name: " + controlName + ", Focus: "
                + control.isFocusControl() );
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

    @Override
    protected void tearDown()
        throws Exception
    {
        try
        {
            if ( wizard != null )
            {
                wizard.dispose();
            }
        }
        finally
        {
            super.tearDown();
        }
    }
}
