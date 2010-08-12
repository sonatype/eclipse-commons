/**
 * 
 */
package org.maven.ide.eclipse.ui.common.test;

import junit.framework.TestCase;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.authentication.RealmComposite;

public class RealmCompositeTest
    extends TestCase
{
    private RealmComposite realmComposite;

    private DummyDialog dialog;

    @Override
    public void setUp() throws Exception 
    {
        dialog = new DummyDialog( new Shell( Display.getCurrent() ) );
        dialog.open();
        super.setUp();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        realmComposite = null;
        dialog.close();
        super.tearDown();
    }

    /*
     * When the RealmComposite was marked as dirty, but no realm had been selected, calling getSecurityRealmURLAssoc()
     * resulted in an NPE. See MECLIPSE-1486
     */
    @Test
    public void testNPEDirtyNoSelection()
    {
        realmComposite.setDirty();
        assertNull( realmComposite.getSecurityRealmURLAssoc() );
    }

    private class DummyDialog
        extends StatusDialog
    {

        public DummyDialog( Shell parent )
        {
            super( parent );
            setBlockOnOpen( false );
        }

        @Override
        protected Control createDialogArea( Composite parent )
        {
            Composite c = (Composite) super.createDialogArea( parent );
            realmComposite =
                new RealmComposite( parent, new Text( parent, SWT.NONE ),
                                    SwtValidationGroup.create( SwtValidationUI.createUI( this ) ), false );
            return c;
        }
    }
}
