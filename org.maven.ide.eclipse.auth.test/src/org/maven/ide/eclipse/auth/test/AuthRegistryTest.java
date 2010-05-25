package org.maven.ide.eclipse.auth.test;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.PBEKeySpec;

import junit.framework.TestCase;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.maven.ide.eclipse.auth.IAuthData;
import org.maven.ide.eclipse.auth.IAuthRealm;
import org.maven.ide.eclipse.auth.internal.AuthRegistry;


public class AuthRegistryTest
    extends TestCase
{

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

    public void testGetRealm()
    {
        String realmId = "http://www.sonatype.com/";

        IAuthRealm realm = registry.addRealm( realmId );
        assertSame( realm, registry.getRealm( realmId ) );
    }

    public void testSelectRealmIdEqualsUrl()
    {
        String realmId = "http://www.sonatype.com/";

        IAuthRealm realm = registry.addRealm( realmId );
        realm.setUsername( "testuser" );
        IAuthData data = realm.getAuthData();
        assertNotNull( data );

        assertSame( data, registry.select( URI.create( realmId ) ) );
    }

    public void testSelectRealmIdIsPrefixOfUrl()
    {
        String realmId = "http://www.sonatype.com";

        IAuthRealm realm = registry.addRealm( realmId );
        realm.setUsername( "testuser" );
        IAuthData data = realm.getAuthData();
        assertNotNull( data );

        assertSame( data, registry.select( URI.create( realmId + "/catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "/sub/dir/catalog.xml" ) ) );
    }

    public void testSelectUrlWithDoubleSlash_MECLIPSE546()
    {
        String realmId = "http://www.sonatype.com//x";

        IAuthRealm realm = registry.addRealm( realmId );
        realm.setUsername( "testuser" );
        IAuthData data = realm.getAuthData();
        assertNotNull( data );

        assertSame( data, registry.select( URI.create( realmId ) ) );
        assertSame( data, registry.select( URI.create( realmId + "/catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "/sub/dir/catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "/dir//catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "//dir//catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "//sub//dir//catalog.xml" ) ) );
    }

    public void testSelectUrlWithDoubleSlash_MECLIPSE546_1()
    {
        String realmId = "http://www.sonatype.com//x";

        IAuthRealm realm = registry.addRealm( realmId, "http://www.sonatype.org" );
        realm.setUsername( "testuser" );
        IAuthData data = realm.getAuthData();
        assertNotNull( data );

        assertSame( data, registry.select( URI.create( realmId ) ) );
        assertSame( data, registry.select( URI.create( realmId + "/catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "/sub/dir/catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "/dir//catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "//dir//catalog.xml" ) ) );
        assertSame( data, registry.select( URI.create( realmId + "//sub//dir//catalog.xml" ) ) );
    }

    public void testGetRealmDoubleSlash_MECLIPSE54()
    {
        String realmId = "http://www.sonatype.com//x";

        IAuthRealm realm = registry.addRealm( realmId );
        assertSame( realm, registry.getRealm( realmId ) );
        assertSame( realm, registry.getRealm( "http://www.sonatype.com/x" ) );
    }

    public void testPersistence()
        throws Exception
    {
        ISecurePreferences preferences = registry.getSecureStorage();

        IAuthRealm realm1 = registry.addRealm( "id-1", "http://www.sonatype.com/" );
        realm1.setUsername( "user-1" );
        realm1.setPassword( "pass-1" );

        IAuthRealm realm2 = registry.addRealm( "id-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", "http://www.sonatype.org/" );
        realm2.setUsername( "user-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]" );
        realm2.setPassword( "pass-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]" );

        IAuthRealm realm3 = registry.addRealm( "id-3" );

        registry = new AuthRegistry( preferences );

        realm1 = registry.getRealm( "id-1" );
        assertNotNull( realm1 );
        assertSame( realm1, registry.getRealm( "http://www.sonatype.com/" ) );
        assertEquals( "id-1", realm1.getId() );
        assertEquals( "user-1", realm1.getAuthData().getUsername() );
        assertEquals( "pass-1", realm1.getAuthData().getPassword() );

        realm2 = registry.getRealm( "id-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]" );
        assertNotNull( realm2 );
        assertSame( realm2, registry.getRealm( "http://www.sonatype.org/" ) );
        assertEquals( "id-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", realm2.getId() );
        assertEquals( "user-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", realm2.getAuthData().getUsername() );
        assertEquals( "pass-_!\"�$%&/()=?\\+*~#',;.:<>|{}[]", realm2.getAuthData().getPassword() );

        realm3 = registry.getRealm( "id-3" );
        assertNotNull( realm3 );
        assertTrue( realm3.isAnonymous() );
    }

    public void testPersistenceWithGracefulRecoveryFromBadStorageState()
        throws Exception
    {
        ISecurePreferences preferences = registry.getSecureStorage();

        IAuthRealm realm1 = registry.addRealm( "id-1", "http://www.sonatype.com/" );
        realm1.setUsername( "user-1" );
        realm1.setPassword( "pass-1" );

        ISecurePreferences authNode = preferences.node( preferences.childrenNames()[0] );
        authNode.put( "mapping-to-missing-realm", "bWlzc2luZw==", false );
        authNode.put( "bad-realm-node-name", "l(-_!)zc2luZw\\", false );

        registry = new AuthRegistry( preferences );

        realm1 = registry.getRealm( "id-1" );
        assertNotNull( realm1 );
        assertSame( realm1, registry.getRealm( "http://www.sonatype.com/" ) );
        assertEquals( "id-1", realm1.getId() );
        assertEquals( "user-1", realm1.getAuthData().getUsername() );
        assertEquals( "pass-1", realm1.getAuthData().getPassword() );

        assertNotNull( registry.addRealm( "mapping-to-missing-realm" ) );
    }

}
