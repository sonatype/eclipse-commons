package org.maven.ide.eclipse.ui.common.authentication;

import java.io.File;

import junit.framework.Assert;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.tests.common.AbstractWizardPageTest;

public class UrlInputCompositeTest
    extends AbstractWizardPageTest
{
    private WizardDialog dialog;

    private Display display;

    public void testNullUrlDisallowCertificate()
    {
        int style = UrlInputComposite.ALLOW_ANONYMOUS;
        DummyPage page = createTestPage( null /* url */, style );
        assertIt( page, "", new AuthData( "", "", AnonymousAccessType.ALLOWED ), style, "URL may not be empty" );
    }

    public void testNullUrlAllowCertificate()
    {
        int style = UrlInputComposite.CERTIFICATE_CONTROLS | UrlInputComposite.ALLOW_ANONYMOUS;
        DummyPage page = createTestPage( null /* url */, style );
        assertIt( page, "", new AuthData( "", "", null, null, AnonymousAccessType.ALLOWED ), style,
                  "Certificate filename may not be empty" );
    }

    public void testUrlWithUsernamePassword()
    {
        String url = "http://UrlInputCompositeTest/testUrlWithUsernamePassword";
        IAuthData authData =
            new AuthData( "username testUrlWithUsernamePassword", "password testUrlWithUsernamePassword",
                          AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthService().save( url, authData );
        int style = 0;
        DummyPage page = createTestPage( url, style );
        assertIt( page, url, authData, style );
    }

    public void testUrlWithUsernamePasswordAnonymousNotAllowed()
    {
        String url =
            "http://UrlInputCompositeTest/testUrlWithUsernamePasswordAnonymousNotAllowed_" + System.currentTimeMillis();
        IAuthData authData = new AuthData( "", "", AnonymousAccessType.NOT_ALLOWED );
        // AuthFacade.getAuthService().save( url, authData );
        int style = 0;
        DummyPage page = createTestPage( url, style );
        assertIt( page, url, authData, style, "Username may not be empty" );
        authData =
            new AuthData( "username testUrlWithUsernamePasswordAnonymousNotAllowed",
                          "password testUrlWithUsernamePasswordAnonymousNotAllowed", AnonymousAccessType.NOT_ALLOWED );
        AuthFacade.getAuthService().save( url, authData );
        page = createTestPage( url, style );
        assertIt( page, url, authData, style );
    }

    public void testUrlWithUsernamePasswordAnonymousRequired()
    {
        String url = "http://UrlInputCompositeTest/testUrlWithUsernamePasswordAnonymousRequired";
        IAuthData authData = new AuthData( "", "", AnonymousAccessType.REQUIRED );
        AuthFacade.getAuthService().save( url, authData );
        int style = 0;
        DummyPage page = createTestPage( url, style );
        assertIt( page, url, authData, style );
    }

    public void testUrlReadOnlyWithUsernamePassword()
    {
        String url = "http://UrlInputCompositeTest/testUrlReadOnlyWithUsernamePassword";
        IAuthData authData =
            new AuthData( "username testUrlReadOnlyWithUsernamePassword",
                          "password testUrlReadOnlyWithUsernamePassword", AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthService().save( url, authData );
        int style = UrlInputComposite.READ_ONLY_URL;
        DummyPage page = createTestPage( url, style );
        assertIt( page, url, authData, style );
    }

    public void testUrlWithCertificate()
    {
        String url = "http://UrlInputCompositeTest/testUrlWithCertificate";
        IAuthData authData =
            new AuthData( new File( "testUrlWithUsernamePasswordAndCertificate" ),
                          "passphrase testUrlWithUsernamePasswordAndCertificate" );
        AuthFacade.getAuthService().save( url, authData );
        int style = 0;
        DummyPage page = createTestPage( url, style );
        assertIt( page, url, authData, style );
    }

    public void testUrlWithUsernamePasswordAndCertificate()
    {
        String url = "http://UrlInputCompositeTest/testUrlWithUsernamePasswordAndCertificate";
        IAuthData authData =
            new AuthData( "username testUrlWithUsernamePasswordAndCertificate",
                          "password testUrlWithUsernamePasswordAndCertificate",
                          new File( "testUrlWithUsernamePasswordAndCertificate" ),
                          "passphrase testUrlWithUsernamePasswordAndCertificate", AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthService().save( url, authData );
        int style = 0;
        DummyPage page = createTestPage( url, style );
        assertIt( page, url, authData, style );
    }

    public void testInvalidUrl()
    {
        String url = "http://UrlInputCompositeTest/testInvalidUrl\\";
        int style = UrlInputComposite.ALLOW_ANONYMOUS;
        DummyPage page = createTestPage( url, style );
        assertIt( page, url, new AuthData( "", "", AnonymousAccessType.ALLOWED ), style, "'" + url
            + "' is not a valid URL" );
    }
    
    private void assertIt( DummyPage page, String expectedUrl, IAuthData authData, int style )
    {
        assertIt( page, expectedUrl, authData, style, null /* errorMessage */);
    }

    private void assertIt( DummyPage page, String expectedUrl, IAuthData authData, int style, String errorMessage )
    {
        // Verify url control
        Assert.assertEquals( "Incorrect expected URL", expectedUrl, page.urlInputComposite.getUrl() );
        if ( ( style & UrlInputComposite.READ_ONLY_URL ) != 0 )
        {
            assertText( page, UrlInputComposite.URL_CONTROL_NAME, expectedUrl, false /* isEnabled */, true /* isVisible */);
        }
        else
        {
            assertCombo( page, UrlInputComposite.URL_CONTROL_NAME, expectedUrl, true /* isEnabled */);
        }

        // Verify username and password controls
        assertText( page, UrlInputComposite.USERNAME_TEXT_NAME, authData.getUsername(),
                    authData.allowsUsernameAndPassword()
                        && !AnonymousAccessType.REQUIRED.equals( authData.getAnonymousAccessType() ),
                    authData.allowsUsernameAndPassword() );
        assertText( page, UrlInputComposite.PASSWORD_TEXT_NAME, authData.getPassword(),
                    authData.allowsUsernameAndPassword()
                        && !AnonymousAccessType.REQUIRED.equals( authData.getAnonymousAccessType() ),
                    authData.allowsUsernameAndPassword() );
        assertLabel( page, UrlInputComposite.ANONYMOUS_LABEL_NAME,
                     authData.allowsUsernameAndPassword() && authData.allowsAnonymousAccess() );

        // Verify certificate controls
        String certificatePath = null;
        if ( authData.getCertificatePath() != null )
        {
            certificatePath = authData.getCertificatePath().getAbsolutePath();
        }
        if ( certificatePath == null )
        {
            certificatePath = "";
        }
        String certificatePassphrase = authData.getCertificatePassphrase();
        if ( certificatePassphrase == null )
        {
            certificatePassphrase = "";
        }
        assertText( page, UrlInputComposite.CERTIFICATE_TEXT_NAME, certificatePath, authData.allowsCertificate(),
                    authData.allowsCertificate() );
        assertText( page, UrlInputComposite.PASSPHRASE_TEXT_NAME, certificatePassphrase, authData.allowsCertificate(),
                    authData.allowsCertificate() );
        assertButton( page, UrlInputComposite.BROWSE_CERTIFICATE_BUTTON_NAME, authData.allowsCertificate(),
                      authData.allowsCertificate() );

        // Verify error/validation message
        assertEquals( "error/validation message is incorrect", errorMessage, page.getMessage() );

        // Verify saved authentication data
        IAuthData savedAuthData = AuthFacade.getAuthService().select( expectedUrl );
        if ( errorMessage != null && errorMessage.trim().length() > 0 )
        {
            assertNull( "Saved authentication data is incorrect: expected null", savedAuthData );
        }
        else
        {
            assertEquals( "Saved authentication data is incorrect", authData, savedAuthData );
        }
    }

    private static class DummyWizard
        extends Wizard
    {
        @Override
        public boolean performFinish()
        {
            return false;
        }
    }

    private static class DummyPage
        extends WizardPage
    {
        private String url;

        private int style;

        UrlInputComposite urlInputComposite;

        protected DummyPage( String url, int style )
        {
            super( "" );
            this.url = url;
            this.style = style;
        }

        public void createControl( Composite parent )
        {
            WidthGroup widthGroup = new WidthGroup();
            SwtValidationGroup validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
            urlInputComposite = new UrlInputComposite( parent, widthGroup, validationGroup, style );
            setControl( urlInputComposite );
            urlInputComposite.setUrl( url );
        }

        @Override
        public void setMessage( String newMessage, int newType )
        {
            super.setMessage( newMessage, newType );
        }
    }

    private DummyPage createTestPage( String url, int urlInputStyle )
    {
        if ( wizard != null )
        {
            wizard.dispose();
        }
        wizard = new DummyWizard();
        DummyPage page = new DummyPage( url, urlInputStyle );
        wizard.addPage( page );

        display = Display.getCurrent();
        if ( display == null )
        {
            display = new Display();
        }
        Shell shell = new Shell( display );

        dialog = new WizardDialog( shell, wizard );
        dialog.create();
        // dialog.open();
        return page;
    }
}
