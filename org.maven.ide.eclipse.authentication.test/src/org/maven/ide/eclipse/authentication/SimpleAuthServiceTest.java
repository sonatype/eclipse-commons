package org.maven.ide.eclipse.authentication;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.PBEKeySpec;

import junit.framework.TestCase;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.maven.ide.eclipse.authentication.internal.SimpleAuthService;

public class SimpleAuthServiceTest
    extends TestCase
{
    // private static final IProgressMonitor monitor = new NullProgressMonitor();

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
}
