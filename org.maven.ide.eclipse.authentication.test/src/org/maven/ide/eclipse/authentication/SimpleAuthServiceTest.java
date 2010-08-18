package org.maven.ide.eclipse.authentication;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.PBEKeySpec;

import junit.framework.TestCase;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.authentication.internal.SimpleAuthService;

public class SimpleAuthServiceTest
    extends TestCase
{
    // private static final IProgressMonitor monitor = new NullProgressMonitor();

    public void testSelectShortUrl()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        assertNull( service.select( "a" ) );
        assertNull( service.select( "aa" ) );
        assertNull( service.select( "aaa" ) );
    }

    public void testNullUrl()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String sUri = null;
        service.save( sUri, "username", "password" );
        assertNull( service.select( sUri ) );
    }

    public void testEmptyUrl()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String sUri = " ";
        service.save( sUri, "username", "password" );
        assertNull( service.select( sUri ) );
        URI uri = new URI( "" );
        assertNull( service.select( uri ) );
    }

    private ISecurePreferences newSecureStorage()
        throws Exception
    {
        File storageFile = File.createTempFile( "s2authtest", ".properties" );
        storageFile.deleteOnExit();
        Map<String, Object> secOpts = new HashMap<String, Object>();
        secOpts.put( IProviderHints.PROMPT_USER, Boolean.FALSE );
        secOpts.put( IProviderHints.DEFAULT_PASSWORD, new PBEKeySpec( new char[] { 't', 'e', 's', 't' } ) );
        return SecurePreferencesFactory.open( storageFile.toURI().toURL(), secOpts );
    }

    public void testSaveAndSelectUsernamePassword()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "http://testSaveAndSelectUsernamePassword";
        assertNull( service.select( url ) );
        service.save( url, "username", "password" );
        IAuthData authData = service.select( url );
        assertNotNull( authData );
        assertEquals( AuthenticationType.USERNAME_PASSWORD, authData.getAuthenticationType() );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );
        assertEquals( AnonymousAccessType.NOT_ALLOWED, authData.getAnonymousAccessType() );
    }

    public void testSaveAndSelectUsernamePasswordAnonymousAllowed()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "http://testSaveAndSelectUsernamePasswordAnonymousAllowed";
        assertNull( service.select( url ) );
        service.save( url, new AuthData( "username", "password", AnonymousAccessType.ALLOWED ) );
        IAuthData authData = service.select( url );
        assertNotNull( authData );
        assertEquals( AuthenticationType.USERNAME_PASSWORD, authData.getAuthenticationType() );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );
        assertEquals( AnonymousAccessType.ALLOWED, authData.getAnonymousAccessType() );
    }

    public void testSaveAndSelectCertificate()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "http://testSaveAndSelectCertificate";
        assertNull( service.select( url ) );
        service.save( url, new File( "foocertificate" ), "passphrase" );
        IAuthData authData = service.select( url );
        assertNotNull( authData );
        assertEquals( AuthenticationType.CERTIFICATE, authData.getAuthenticationType() );
        assertEquals( new File( "foocertificate" ).getAbsolutePath(), authData.getCertificatePath().getAbsolutePath() );
        assertEquals( "passphrase", authData.getCertificatePassphrase() );
    }

    public void testSaveAndSelectUsernamePasswordAndCertificate()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "http://testSaveAndSelectUsernamePasswordAndCertificate";
        assertNull( service.select( url ) );
        IAuthData authData =
            new AuthData( "username", "password", new File( "foocertificate" ), "passphrase",
                          AnonymousAccessType.NOT_ALLOWED );
        service.save( url, authData );
        authData = service.select( url );
        assertNotNull( authData );
        assertEquals( AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD, authData.getAuthenticationType() );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );
        assertEquals( new File( "foocertificate" ).getAbsolutePath(), authData.getCertificatePath().getAbsolutePath() );
        assertEquals( "passphrase", authData.getCertificatePassphrase() );
    }

    public void testSaveAndSelectNoEndSlash()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );
        service.save( "http://foo/bar", "username", "password" );

        IAuthData authData = service.select( "http://foo/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        // Get a new SimpleAuthService for the same secure storage
        service = new SimpleAuthService( secureStorage );
        authData = service.select( "http://foo/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );
    }

    public void testSaveAndSelectWithEndSlash()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );
        service.save( "http://foo/bar/", "username", "password" );

        IAuthData authData = service.select( "http://foo/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        // Get a new SimpleAuthService for the same secure storage
        service = new SimpleAuthService( secureStorage );
        authData = service.select( "http://foo/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );

        authData = service.select( "http://foo/bar/bar/" );
        assertNotNull( authData );
        assertEquals( "username", authData.getUsername() );
        assertEquals( "password", authData.getPassword() );
    }

    public void testWindowsStyleFileUrl()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "file:/c:\\foo";
        service.save( url, "username", "password" );
        assertNotNull( service.select( url ) );
    }

    public void testScmUrl()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "scm:git:ssh://localhost:4807/foo";
        service.save( url, "username", "password" );
        assertNotNull( service.select( url ) );
    }

    public void testInvalidScmUrl()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "scm:ssh://localhost:4807/foo";
        try
        {
            service.save( url, "username", "password" );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !"SCM URI 'scm:ssh://localhost:4807/foo'does not specify SCM type".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
        assertNull( service.select( url ) );
    }

    public void testSaveAuthDataOnlyWhenChanged_UsernamePassword()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "http://SimpleAuthServiceTest/testSaveAuthDataOnlyWhenChanged_UsernamePassword/";
        String username = "username";
        String password = "password";
        AnonymousAccessType anonymousAccessType = AnonymousAccessType.NOT_ALLOWED;
        IAuthData authData = new AuthData( username, password, anonymousAccessType );
        assertTrue( service.save( url, authData ) );
        authData = service.select( url );

        authData = new AuthData( username, password, anonymousAccessType );
        assertFalse( service.save( url, authData ) );

        username += "x";
        authData = new AuthData( username, password, anonymousAccessType );
        assertTrue( service.save( url, authData ) );

        password += "x";
        authData = new AuthData( username, password, anonymousAccessType );
        assertTrue( service.save( url, authData ) );
    }

    public void testSaveAuthDataOnlyWhenChanged_Certificate()
        throws Exception
    {
        ISecurePreferences secureStorage = newSecureStorage();
        SimpleAuthService service = new SimpleAuthService( secureStorage );

        String url = "http://SimpleAuthServiceTest/testSaveAuthDataOnlyWhenChanged_Certificate/";
        File certificate = new File( "foo" );
        String passphrase = "passphrase";
        IAuthData authData = new AuthData( certificate, passphrase );
        assertTrue( service.save( url, authData ) );
        authData = service.select( url );

        authData = new AuthData( certificate, passphrase );
        assertFalse( service.save( url, authData ) );

        certificate = new File( "foox" );
        authData = new AuthData( certificate, passphrase );
        assertTrue( service.save( url, authData ) );

        passphrase += "x";
        authData = new AuthData( certificate, passphrase );
        assertTrue( service.save( url, authData ) );
    }
}
