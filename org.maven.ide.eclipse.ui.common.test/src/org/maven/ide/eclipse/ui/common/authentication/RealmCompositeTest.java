package org.maven.ide.eclipse.ui.common.authentication;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthRegistryException;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.IAuthRegistry;
import org.maven.ide.eclipse.authentication.internal.URIHelper;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.authentication.RealmComposite;
import org.maven.ide.eclipse.ui.tests.common.AbstractWizardPageTest;

public class RealmCompositeTest
    extends AbstractWizardPageTest
{
    private static final String COMPONENT_NAME = "URL text";

    private static final String EMPTY = "";

    private static final String INVALID = "http:";

    private static final String UNKNOWN = "http://unknown";

    private static final String KNOWN = "http://known";

    private static final String REALM = "realm";

    private Text text;

    private DummyRealmComposite realmComposite;

    private WizardDialog dialog;

    private WizardPage page;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        Display display = Display.getCurrent();
        if ( display == null )
        {
            display = new Display();
        }
        dialog = new DummyDialog( new Shell( display ) );
        dialog.open();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        dialog.close();
        dialog = null;
        super.tearDown();
    }

    public void testDisabled()
    {
        text.setEnabled( false );
        text.setText( EMPTY );
        assertFalse( "Realm composite should be disabled if the URL field is disabled",
                     realmComposite.getTextControl().isEnabled() );
        assertEquals( "Realm composite should be empty if the URL field is disabled", "",
                      realmComposite.getTextControl().getText() );
        assertNull( "No warning message should be displayed if the URL field is disabled", page.getMessage() );
    }

    public void testEmpty()
    {
        text.setEnabled( true );
        text.setText( EMPTY );
        assertFalse( "Realm composite should be disabled if the URL field is empty",
                     realmComposite.getTextControl().isEnabled() );
        assertEquals( "Realm composite should be empty if the URL field is empty", "",
                      realmComposite.getTextControl().getText() );
        assertNull( "No warning message should be displayed if the URL field is empty", page.getMessage() );
    }

    public void testInvalid()
    {
        try
        {
            URIHelper.normalize( INVALID );
            fail( "Expected an invalid URL" );
        }
        catch ( AuthRegistryException e )
        {
            // this is in fact an invalid URL

            text.setEnabled( true );
            text.setText( INVALID );
            assertFalse( "Realm composite should be disabled if the URL is invalid",
                         realmComposite.getTextControl().isEnabled() );
            assertEquals( "Realm composite should be empty if the URL is invalid", "",
                          realmComposite.getTextControl().getText() );
            assertNull( "No warning message should be displayed if the URL is invalid", page.getMessage() );
        }
    }

    public void testUnknown()
    {
        text.setEnabled( true );
        text.setText( UNKNOWN );
        assertTrue( "Realm composite should be enabled if the URL field is valid and unknown",
                    realmComposite.getTextControl().isEnabled() );
        assertEquals( "Realm composite should be empty if the URL field is unknown", "",
                      realmComposite.getTextControl().getText() );
        assertEquals(
                      "A warning message containing the URL control name should be displayed if the URL field is unknown",
                      NLS.bind( Messages.realmComposite_selectRealmFor, COMPONENT_NAME ), page.getMessage() );
        assertEquals( "An INFO icon should be displayed if the URL field is unknown", IMessageProvider.INFORMATION,
                      page.getMessageType() );
    }

    public void testKnown()
    {
        IAuthRegistry registry = AuthFacade.getAuthRegistry();
        IAuthRealm realm = registry.addRealm( REALM, REALM, REALM, AuthenticationType.USERNAME_PASSWORD, monitor );
        registry.addURLToRealmAssoc( KNOWN, REALM, AnonymousAccessType.ALLOWED, monitor );

        text.setEnabled( true );
        text.setText( KNOWN );
        assertFalse( "Realm composite should be disabled if the URL has a matching realm",
                     realmComposite.getTextControl().isEnabled() );
        assertEquals( "Realm composite should display the realm name if the URL has a matching realm", realm.getName(),
                      realmComposite.getTextControl().getText() );
        assertNull( "No warning message should be displayed if the URL has a matching realm", page.getMessage() );
    }

    public void testEdit()
    {
        testUnknown();
        
        List list = realmComposite.getList();
        Event event = new Event();
        event.widget = list;
        event.type = SWT.Selection;

        assertFalse( "Realm composite should not be dirty if the value has not been changed", realmComposite.isDirty() );

        realmComposite.showPopup( true );
        assertEquals( "The realm list should have two options", 2, realmComposite.getList().getItemCount() );
        list.select( 1 );
        list.notifyListeners( SWT.Selection, event );
        realmComposite.showPopup( false );

        assertTrue( "Realm composite should be dirty when a realm is selected", realmComposite.isDirty() );
        assertEquals( "Realm composite should display the realm name when a realm is selected", REALM,
                      realmComposite.getTextControl().getText() );
        assertNull( "No warning message should be displayed once a realm is selected", page.getMessage() );

        realmComposite.showPopup( true );
        list.select( 0 );
        list.notifyListeners( SWT.Selection, event );
        realmComposite.showPopup( false );

        assertFalse( "Realm composite should not be dirty once selection is reset", realmComposite.isDirty() );
        assertEquals( "Realm composite should be empty once selection is reset", "",
                      realmComposite.getTextControl().getText() );
        assertEquals( "A warning message containing the URL control name should be displayed once selection is reset",
                      NLS.bind( Messages.realmComposite_selectRealmFor, COMPONENT_NAME ), page.getMessage() );
        assertEquals( "An INFO icon should be displayed once selection is reset", IMessageProvider.INFORMATION,
                      page.getMessageType() );
    }

    private class DummyPage
        extends WizardPage
    {
        protected DummyPage()
        {
            super( "page" );
        }

        public void createControl( Composite parent )
        {
            Composite c = new Composite( parent, SWT.NONE );
            c.setLayout( new GridLayout() );

            text = new Text( c, SWT.NONE );
            SwtValidationGroup.setComponentName( text, COMPONENT_NAME );

            realmComposite =
                new DummyRealmComposite( c, text, SwtValidationGroup.create( SwtValidationUI.createUI( this ) ) );

            setControl( c );
        }
    }

    private class DummyRealmComposite
        extends RealmComposite
    {
        public DummyRealmComposite( Composite parent, Text urlText, SwtValidationGroup validationGroup )
        {
            super( parent, urlText, validationGroup, null );
        }

        @Override
        public List getList()
        {
            // TODO Auto-generated method stub
            return super.getList();
        }
    }

    private class DummyDialog
        extends WizardDialog
    {
        public DummyDialog( Shell parentShell )
        {
            super( parentShell, wizard = new Wizard()
            {
                @Override
                public boolean performFinish()
                {
                    return false;
                }

                @Override
                public void addPages()
                {
                    page = new DummyPage();
                    addPage( page );
                };
            } );
            setBlockOnOpen( false );
        }
    }
}
