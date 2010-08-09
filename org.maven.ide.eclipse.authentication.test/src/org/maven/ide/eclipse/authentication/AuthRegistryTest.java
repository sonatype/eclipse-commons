package org.maven.ide.eclipse.authentication;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.PBEKeySpec;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.maven.ide.eclipse.authentication.internal.AuthRealm;
import org.maven.ide.eclipse.authentication.internal.AuthRegistry;


public class AuthRegistryTest
    extends TestCase
{
    private static final IProgressMonitor monitor = new NullProgressMonitor();

    private AuthRegistry registry;

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

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        registry = new AuthRegistry(newSecureStorage());
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        registry.clear();
        registry = null;

        super.tearDown();
    }

    public void testGetRealmEmpty()
    {
        assertNull( registry.getRealm( "" ) );
    }

    public void testGetRealmNull()
    {
        assertNull( registry.getRealm( null ) );
    }

    public void testAddRealmWithExistingRealmId()
        throws CoreException
    {
        String realmId = "testAddRealmWithExistingRealmId";

        registry.addRealm( realmId, "name", "desc", AuthenticationType.USERNAME_PASSWORD, monitor );
        assertNotNull( registry.getRealm( realmId ) );

        try
        {
            registry.addRealm( realmId, "name", "desc", AuthenticationType.USERNAME_PASSWORD, monitor );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !"A security realm with id='testAddRealmWithExistingRealmId' already exists.".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }

    public void testGetRealm()
        throws CoreException
    {
        String realmId = "http://www.sonatype.com/";

        IAuthRealm realm = registry.addRealm( realmId, "name", "desc", AuthenticationType.USERNAME_PASSWORD, monitor );
        assertSame( realm, registry.getRealm( realmId ) );
    }

    public void testSelectPrefixOfUrl()
        throws Exception
    {
        String realmId = "foo";

        IAuthRealm realm = registry.addRealm( realmId, "name", "desc", AuthenticationType.USERNAME_PASSWORD, monitor );
        realm.setUsernameAndPassword( "testuser", "testpass" );
        registry.addURLToRealmAssoc( "http://foo", realmId, AnonymousAccessType.ALLOWED, monitor );

        IAuthData authData = registry.select( URI.create( "http://foo/catalog.xml" ) );
        assertEquals( "testuser", authData.getUsername() );
        assertEquals( "testpass", authData.getPassword() );
        assertEquals( AuthenticationType.USERNAME_PASSWORD, authData.getAuthenticationType() );
        assertEquals( AnonymousAccessType.ALLOWED, authData.getAnonymousAccessType() );

        authData = registry.select( URI.create( "http://foo/sub/dir/catalog.xml" ) );
        assertEquals( "testuser", authData.getUsername() );
        assertEquals( "testpass", authData.getPassword() );
        assertEquals( AuthenticationType.USERNAME_PASSWORD, authData.getAuthenticationType() );
        assertEquals( AnonymousAccessType.ALLOWED, authData.getAnonymousAccessType() );
    }

    private void assertAuthData( IAuthData authData, String username, String password,
                                 AnonymousAccessType anonymousAccessType )
    {
        assertNotNull( authData );
        assertEquals( username, authData.getUsername() );
        assertEquals( password, authData.getPassword() );
        assertEquals( anonymousAccessType, authData.getAnonymousAccessType() );
        assertEquals( AuthenticationType.USERNAME_PASSWORD, authData.getAuthenticationType() );
    }

    public void testSelectUrlWithDoubleSlash_MECLIPSE546()
        throws Exception
    {
        String realmId = "testSelectUrlWithDoubleSlash_MECLIPSE546";
        String url = "http://www.sonatype.com//x";

        IAuthRealm realm = registry.addRealm( realmId, "name", "desc", AuthenticationType.USERNAME_PASSWORD, monitor );
        realm.setUsernameAndPassword( "testuser", "testpass" );
        registry.addURLToRealmAssoc( url, realmId, AnonymousAccessType.ALLOWED, monitor );

        assertAuthData( registry.select( URI.create( url ) ), "testuser", "testpass", AnonymousAccessType.ALLOWED );
        assertAuthData( registry.select( URI.create( url + "/catalog.xml" ) ), "testuser", "testpass",
                        AnonymousAccessType.ALLOWED );
        assertAuthData( registry.select( URI.create( url + "/sub/dir/catalog.xml" ) ), "testuser", "testpass",
                        AnonymousAccessType.ALLOWED );
        assertAuthData( registry.select( URI.create( url + "/dir//catalog.xml" ) ), "testuser", "testpass",
                        AnonymousAccessType.ALLOWED );
        assertAuthData( registry.select( URI.create( url + "//dir//catalog.xml" ) ), "testuser", "testpass",
                        AnonymousAccessType.ALLOWED );
        assertAuthData( registry.select( URI.create( url + "//sub//dir//catalog.xml" ) ), "testuser", "testpass",
                        AnonymousAccessType.ALLOWED );
    }

    public void testAddRealmIdNull()
        throws Exception
    {
        try
        {
            registry.addRealm( null /* id */, "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddRealmIdEmpty()
        throws Exception
    {
        try
        {
            registry.addRealm( " " /* id */, "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddRealmAuthenticationTypeNull()
        throws Exception
    {
        try
        {
            registry.addRealm( "testAddRealmAuthenticationTypeNull", "realm-name-1", "realm-description-1",
                               null /* AuthenticationType */, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The authentication type of a security realm cannot be null." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateRealmAuthenticationTypeNull()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateRealmAuthenticationTypeNull", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        try
        {
            realm.setAuthenticationType( null );
            registry.updateRealm( realm, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The authentication type of a security realm cannot be null." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddRealmNameNull()
        throws Exception
    {
        try
        {
            registry.addRealm( "realm-id", null /* name */, "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The name of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddRealmNameEmpty()
        throws Exception
    {
        try
        {
            registry.addRealm( "realm-id", " " /* name */, "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The name of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testPersistence_Realms()
        throws Exception
    {
        ISecurePreferences preferences = registry.getSecureStorage();

        // Add
        AuthRealm realm =
            (AuthRealm) registry.addRealm( "realm-id-1", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        realm.setUsernameAndPassword( "user", "pass" );

        registry.addRealm( "id-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", "realm-name-2", "realm-description-2",
                           AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD, monitor );

        registry = new AuthRegistry( preferences );

        realm = (AuthRealm) registry.getRealm( "realm-id-1" );
        assertNotNull( realm );
        assertEquals( "realm-id-1", realm.getId() );
        assertEquals( "realm-name-1", realm.getName() );
        assertEquals( "realm-description-1", realm.getDescription() );
        assertEquals( AuthenticationType.USERNAME_PASSWORD, realm.getAuthenticationType() );
        assertEquals( "user", realm.getUsername() );
        assertEquals( "pass", realm.getPassword() );
        realm.setUsernameAndPassword( "user-1", "pass-1" );
        assertEquals( "user-1", realm.getUsername() );
        assertEquals( "pass-1", realm.getPassword() );

        realm = (AuthRealm) registry.getRealm( "id-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]" );
        assertNotNull( realm );
        assertEquals( "id-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", realm.getId() );
        assertEquals( "realm-name-2", realm.getName() );
        assertEquals( "realm-description-2", realm.getDescription() );
        assertEquals( AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD, realm.getAuthenticationType() );
        realm.setUsernameAndPassword( "user-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]",
                                      "pass-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]" );
        assertEquals( "user-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", realm.getUsername() );
        assertEquals( "pass-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", realm.getPassword() );

        // Update
        realm = (AuthRealm) registry.getRealm( "realm-id-1" );
        realm.setName( "realm-name-updated" );
        registry.updateRealm( realm, monitor );
        registry = new AuthRegistry( preferences );
        realm = (AuthRealm) registry.getRealm( "realm-id-1" );
        assertNotNull( realm );
        assertEquals( "realm-name-updated", realm.getName() );

        // Remove
        registry.removeRealm( "realm-id-1", monitor );
        registry = new AuthRegistry( preferences );
        assertNull( registry.getRealm( "realm-id-1" ) );
    }

    public void testPersistence_URLToRealmAssocs()
        throws Exception
    {
        ISecurePreferences preferences = registry.getSecureStorage();
        AuthRealm realm =
            (AuthRealm) registry.addRealm( "realm-id-1", "realm-name-1", "realm-description-1",
                                           AuthenticationType.USERNAME_PASSWORD, monitor );
        realm.setUsernameAndPassword( "user", "pass" );
        String realmId = realm.getId();

        // Add
        String url = "http://testPersistence_URLToRealmAssocs";
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( url, realmId, AnonymousAccessType.NOT_ALLOWED, monitor );
        String urlAssocId = urlAssoc.getId();
        assertNotNull( urlAssocId );
        registry = new AuthRegistry( preferences );
        urlAssoc = registry.getURLToRealmAssoc( urlAssocId );
        assertNotNull( urlAssoc );
        assertNotNull( urlAssoc.getId() );
        assertEquals( url, urlAssoc.getUrl() );
        assertEquals( realmId, urlAssoc.getRealmId() );
        assertEquals( AnonymousAccessType.NOT_ALLOWED, urlAssoc.getAnonymousAccess() );

        // Update
        String newUrl = "http://testPersistence_URLToRealmAssocs";
        urlAssoc.setUrl( newUrl );
        registry.updateURLToRealmAssoc( urlAssoc, monitor );
        registry = new AuthRegistry( preferences );
        urlAssoc = registry.getURLToRealmAssoc( urlAssocId );
        assertNotNull( urlAssoc );
        assertEquals( newUrl, urlAssoc.getUrl() );

        // Remove
        registry.removeURLToRealmAssoc( urlAssocId, monitor );
        registry = new AuthRegistry( preferences );
        assertNull( registry.getURLToRealmAssoc( urlAssocId ) );
    }

    public void testOrphanURL()
        throws Exception
    {
        String url = "http://testOrphanURL";
        IAuthData authData = registry.select( url );
        assertNull( authData );

        registry.save( url, "u", "p" );
        authData = registry.select( url );
        assertNotNull( authData );
        assertEquals( "u", authData.getUsername() );
        assertEquals( "p", authData.getPassword() );
    }

    public void testWindowsStyleFileUrl()
    {
        String realmId = "testWindowsStyleFileUrl";
        String url = "file:/c:\\foo";
        registry.addRealm( realmId, "realm-name-1", "realm-description-1", AuthenticationType.USERNAME_PASSWORD,
                           monitor );
        registry.addURLToRealmAssoc( url, realmId, AnonymousAccessType.ALLOWED, monitor );
        registry.save( url, "username", "password" );
        assertNotNull( registry.select( url ) );
    }

    public void testScmUrl()
    {
        String realmId = "testScmUrl";
        String url = "scm:git:ssh://localhost:4807/foo";
        registry.addRealm( realmId, "realm-name-1", "realm-description-1",
                           AuthenticationType.USERNAME_PASSWORD, monitor );
        registry.addURLToRealmAssoc( url, realmId, AnonymousAccessType.ALLOWED,
                                     monitor );
        registry.save( url, "username", "password" );
        assertNotNull( registry.select( url ) );
    }

    public void testInvalidScmUrl()
    {
        String realmId = "testInvalidScmUrl";
        String url = "scm:ssh://localhost:4807/foo";
        registry.addRealm( realmId, "realm-name-1", "realm-description-1", AuthenticationType.USERNAME_PASSWORD,
                           monitor );
        try
        {
            registry.addURLToRealmAssoc( url, realmId, AnonymousAccessType.ALLOWED, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !"SCM URI 'scm:ssh://localhost:4807/foo'does not specify SCM type".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
        try
        {
            registry.save( url, "username", "password" );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !"SCM URI 'scm:ssh://localhost:4807/foo'does not specify SCM type".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
        assertNull( registry.select( url ) );
    }

    public void testGetRealmForURI()
        throws Exception
    {
        assertNull( registry.getRealmForURI( "http://testGetRealmForURI" ) );

        String realmId = "testGetRealmForURI-realmid";

        IAuthRealm realm = registry.addRealm( realmId, "name", "desc", AuthenticationType.USERNAME_PASSWORD, monitor );
        registry.addURLToRealmAssoc( "http://testGetRealmForURI", realmId, AnonymousAccessType.ALLOWED, monitor );

        realm = registry.getRealmForURI( "http://testGetRealmForURI" );
        assertNotNull( realm );
        assertEquals( realmId, realm.getId() );

        realm = registry.getRealmForURI( "http://testGetRealmForURI/sub/dir/catalog.xml" );
        assertNotNull( realm );
        assertEquals( realmId, realm.getId() );
    }

    public void testRemoveRealmIdNull()
        throws Exception
    {
        try
        {
            registry.removeRealm( null /* id */, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testRemoveRealmIdEmpty()
        throws Exception
    {
        try
        {
            registry.removeRealm( " " /* id */, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testRemoveRealmIdDoesNotExist()
        throws Exception
    {
        String realmId = "testRemoveRealmIdDoesNotExist";
        try
        {
            registry.removeRealm( realmId, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "A security realm with id='" + realmId + "' does not exists." ) )
            {
                throw expected;
            }
        }
    }

    public void testRemoveRealm()
        throws CoreException
    {
        String realmId = "testRemoveRealm";

        registry.addRealm( realmId, "name", "desc", AuthenticationType.USERNAME_PASSWORD, monitor );
        assertNotNull( registry.getRealm( realmId ) );

        registry.removeRealm( realmId, monitor );
        assertNull( registry.getRealm( realmId ) );
    }

    public void testUpdateRealmIdNull()
        throws Exception
    {
        IAuthRealm realm = new AuthRealm( null /* id */, "realm-name", "desc", AuthenticationType.CERTIFICATE );
        try
        {
            registry.updateRealm( realm, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateRealmIdEmpty()
        throws Exception
    {
        IAuthRealm realm = new AuthRealm( " " /* id */, "realm-name", "desc", AuthenticationType.CERTIFICATE );
        try
        {
            registry.updateRealm( realm, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateRealmNameNull()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateRealmNameNull", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD,
                               monitor );
        realm.setName( null );
        try
        {
            registry.updateRealm( realm, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The name of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateRealmNameEmpty()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateRealmNameEmpty", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD,
                               monitor );
        realm.setName( " " );
        try
        {
            registry.updateRealm( realm, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The name of a security realm cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateRealm()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateRealm", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        realm.setName( "realm-name-updated" );
        registry.updateRealm( realm, monitor );
        realm = registry.getRealm( "testUpdateRealm" );
        assertEquals( "realm-name-updated", realm.getName() );
    }

    public void testAddUrlNull()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testAddUrlNull", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        try
        {
            registry.addURLToRealmAssoc( null /* url */, realm.getId(), AnonymousAccessType.NOT_ALLOWED, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The url cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddUrlEmpty()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testAddUrlEmpty", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        try
        {
            registry.addURLToRealmAssoc( " " /* url */, realm.getId(), AnonymousAccessType.NOT_ALLOWED, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The url cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddUrlRealmIdNull()
        throws Exception
    {
        try
        {
            registry.addURLToRealmAssoc( "http://testAddUrlRealmIdNull", null /* realmid */,
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The realm id cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddUrlRealmIdEmpty()
        throws Exception
    {
        try
        {
            registry.addURLToRealmAssoc( "http://testAddUrlRealmIdEmpty", " " /* realmid */,
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The realm id cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddUrlAnonymousAccessTypeNull()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testAddUrlAnonymousAccessTypeNull", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        try
        {
            registry.addURLToRealmAssoc( "http://testAddUrlRealmIdNull", realm.getId(), null /* AnonymousAccessType */,
                                         monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The anonymousAccessType cannot be null." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddUrlWithEndSlash()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testAddUrlWithEndSlash", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testAddUrlWithEndSlash/", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        assertEquals( "http://testAddUrlWithEndSlash", urlAssoc.getUrl() );
    }

    public void testAddUrlAlreadyExists()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testAddUrlAlreadyExists", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        String url = "http://testAddUrlAlreadyExists";
        registry.addURLToRealmAssoc( url, realm.getId(), AnonymousAccessType.NOT_ALLOWED, monitor );
        try
        {
            registry.addURLToRealmAssoc( url, realm.getId(), AnonymousAccessType.NOT_ALLOWED, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The '" + url + "' URL is already associated with a security realm." ) )
            {
                throw expected;
            }
        }
    }

    public void testAddUrlRealmIdDoesNotExist()
        throws Exception
    {
        String realmId = "testAddUrlRealmIdDoesNotExist";
        try
        {
            registry.addURLToRealmAssoc( "http://testAddUrlRealmIdDoesNotExist", realmId,
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "A security realm with id='" + realmId + "' does not exist." ) )
            {
                throw expected;
            }
        }
    }

    public void testRemoveUrlIdNull()
        throws Exception
    {
        try
        {
            registry.removeURLToRealmAssoc( null /* urlToRealmAssocId */, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a URL to realm association cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testRemoveUrlIdEmpty()
        throws Exception
    {
        try
        {
            registry.removeURLToRealmAssoc( " " /* urlToRealmAssocId */, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The id of a URL to realm association cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testRemoveUrlIdDoesNotExist()
        throws Exception
    {
        String urlToRealmAssocId = "testRemoveUrlIdDoesNotExist";
        try
        {
            registry.removeURLToRealmAssoc( urlToRealmAssocId, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "A URL to realm association with id='" + urlToRealmAssocId
                                                    + "' does not exists." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlNull()
        throws Exception
    {
        try
        {
            registry.updateURLToRealmAssoc( null /* urlToRealmAssoc */, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The URL to realm association cannot be null." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlUrlNull()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlUrlNull", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlUrlNull", realm.getId(), AnonymousAccessType.NOT_ALLOWED,
                                         monitor );
        urlAssoc.setUrl( null );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The url cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlUrlEmpty()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlUrlEmpty", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlUrlEmpty", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED,
                                         monitor );
        urlAssoc.setUrl( " " );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The url cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlRealmIdNull()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlRealmIdNull", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlRealmIdNull", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        urlAssoc.setRealmId( null );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The realm id cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlRealmIdEmpty()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlRealmIdEmpty", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlRealmIdEmpty", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        urlAssoc.setRealmId( " " );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The realm id cannot be null or empty." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlAnonymousAccessTypeNull()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlAnonymousAccessTypeNull", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlAnonymousAccessTypeNull", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        urlAssoc.setAnonymousAccess( null );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The anonymousAccessType cannot be null." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlDoesNotExist()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlDoesNotExist", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        String urlAssocId = "testUpdateUrlDoesNotExist";
        ISecurityRealmURLAssoc urlAssoc =
            new SecurityRealmURLAssoc( urlAssocId, "http://testUpdateUrlDoesNotExist",
                                       realm.getId(), AnonymousAccessType.NOT_ALLOWED );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "A URL to realm association with id='" + urlAssocId
                                                    + "' does not exist." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlRealmIdDoesNotExist()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlRealmIdDoesNotExist", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlRealmIdDoesNotExist", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        String newRealmId = "testUpdateUrlRealmIdDoesNotExist1";
        urlAssoc.setRealmId( newRealmId );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "A security realm with id='" + newRealmId + "' does not exist." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrlWithEndSlash()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlWithEndSlash", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlWithEndSlash", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        urlAssoc.setUrl( "http://foo/testUpdateUrlWithEndSlash/bar/" );
        registry.updateURLToRealmAssoc( urlAssoc, monitor );
        urlAssoc = registry.getURLToRealmAssoc( urlAssoc.getId() );
        assertEquals( "http://foo/testUpdateUrlWithEndSlash/bar", urlAssoc.getUrl() );
    }

    public void testUpdateUrlWithDoubleSlash()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlWithDoubleSlash", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlWithDoubleSlash", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        urlAssoc.setUrl( "http://foo/testUpdateUrlWithDoubleSlash//bar/" );
        registry.updateURLToRealmAssoc( urlAssoc, monitor );
        urlAssoc = registry.getURLToRealmAssoc( urlAssoc.getId() );
        assertEquals( "http://foo/testUpdateUrlWithDoubleSlash/bar", urlAssoc.getUrl() );
    }

    public void testUpdateUrlAlreadyAssociated()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrlAlreadyAssociated", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        String url = "http://testUpdateUrlAlreadyAssociated";
        registry.addURLToRealmAssoc( url, realm.getId(), AnonymousAccessType.NOT_ALLOWED, monitor );
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( "http://testUpdateUrlAlreadyAssociated1", realm.getId(),
                                         AnonymousAccessType.NOT_ALLOWED, monitor );
        urlAssoc.setUrl( url );
        try
        {
            registry.updateURLToRealmAssoc( urlAssoc, monitor );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals( "The '" + url + "' URL is already associated with a security realm." ) )
            {
                throw expected;
            }
        }
    }

    public void testUpdateUrl()
        throws Exception
    {
        IAuthRealm realm =
            registry.addRealm( "testUpdateUrl", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        String url = "http://testUpdateUrl";
        ISecurityRealmURLAssoc urlAssoc =
            registry.addURLToRealmAssoc( url, realm.getId(), AnonymousAccessType.NOT_ALLOWED, monitor );
        String newUrl = "http://testUpdateUrl1";
        urlAssoc.setUrl( newUrl );
        registry.updateURLToRealmAssoc( urlAssoc, monitor );
        urlAssoc = registry.getURLToRealmAssoc( urlAssoc.getId() );
        assertNotNull( urlAssoc );
        assertEquals( newUrl, urlAssoc.getUrl() );
        assertNull( registry.getRealmForURI( url ) );
        assertNotNull( registry.getRealmForURI( newUrl ) );
    }

    public void testGetRealms()
    {
        Collection<IAuthRealm> realms = registry.getRealms();
        assertNotNull( realms );
        int count = realms.size();
        registry.addRealm( "testGetRealms", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        realms = registry.getRealms();
        assertNotNull( realms );
        assertEquals( count + 1, realms.size() );
    }

    public void testGetUrls()
    {
        Collection<ISecurityRealmURLAssoc> urls = registry.getURLToRealmAssocs();
        assertNotNull( urls );
        int count = urls.size();
        IAuthRealm realm =
            registry.addRealm( "testGetUrls", "realm-name-1", "realm-description-1",
                               AuthenticationType.USERNAME_PASSWORD, monitor );
        registry.addURLToRealmAssoc( "http://testGetUrls", realm.getId(), AnonymousAccessType.NOT_ALLOWED, monitor );
        urls = registry.getURLToRealmAssocs();
        assertNotNull( urls );
        assertEquals( count + 1, urls.size() );
    }
}
