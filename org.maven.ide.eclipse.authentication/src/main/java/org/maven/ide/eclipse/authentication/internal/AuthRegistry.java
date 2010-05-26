package org.maven.ide.eclipse.authentication.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.IAuthRegistry;
import org.maven.ide.eclipse.authentication.IAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthRegistry
    implements IAuthRegistry, IAuthService
{
    private final Logger log = LoggerFactory.getLogger( AuthRegistry.class );

    /**
     * The path to the root node for the auth registry. Each realm will be saved in a child of this node.
     */
    private static final String SECURE_NODE_PATH = "com.sonatype.s2.project.auth.registry";

    /**
     * The key to save the username into.
     */
    private static final String SECURE_USERNAME = "username";

    /**
     * The key to save the password into.
     */
    private static final String SECURE_PASSWORD = "password";

    /**
     * Map of AuthRealm by realm id.
     */
    private Map<String, IAuthRealm> realms = new HashMap<String, IAuthRealm>();

    private final ISecurePreferences secureStorage;

    public AuthRegistry()
    {
        this( SecurePreferencesFactory.getDefault() );
    }

    /**
     * Creates a new authentication registry backed by the specified secure storage.
     * 
     * @param secureStorage The (root node of the) secure storage to load/save the registry from/to, may be {@code null}
     *            .
     */
    public AuthRegistry( ISecurePreferences secureStorage )
    {
        this.secureStorage = secureStorage;
        load();
    }

    public ISecurePreferences getSecureStorage()
    {
        return secureStorage;
    }

    private String normalizeRealmId( String realmId )
    {
        URI uri;
        try
        {
            uri = URI.create( realmId );
            uri = uri.normalize();
        }
        catch ( IllegalArgumentException e )
        {
            // Not a URI, that's fine
            return realmId;
        }
        realmId = uri.toString();
        return stripSlash( realmId );
    }

    public IAuthRealm addRealm( String realmId )
    {
        realmId = normalizeRealmId( realmId );

        IAuthRealm realm = realms.get( realmId );
        if ( realm != null )
        {
            log.debug( "Skipped re-addition of authentication realm {}", realmId );
            return realm;
        }

        log.debug( "Adding new authentication realm {}", realmId );
        realm = new AuthRealm( realmId );
        realms.put( realmId, realm );

        ( (AuthRealm) realm ).save( realmId );

        return realm;
    }

    public IAuthRealm addRealm( String realmId, String url )
    {
        realmId = normalizeRealmId( realmId );

        IAuthRealm realm = realms.get( realmId );
        if ( realm == null )
        {
            log.debug( "Adding new authentication realm {}", realmId );
            realm = new AuthRealm( realmId );
            realms.put( realmId, realm );
        }
        else
        {
            log.debug( "Skipped re-addition of authentication realm {}", realmId );
        }

        String urlId = normalizeRealmId( url );
        log.debug( "Mapping {} to authentication realm {}", urlId, realm );
        realms.put( urlId, realm );

        ( (AuthRealm) realm ).save( realmId );
        ( (AuthRealm) realm ).save( urlId );

        return realm;
    }

    public void mapUrlToRealm( String realmId, String url )
        throws URISyntaxException
    {
        realmId = normalizeRealmId( realmId );
        IAuthRealm realm = addRealm( realmId );

        url = normalizeRealmId( url );
        AuthRealm realmForUrl;
        try
        {
            realmForUrl = (AuthRealm) findRealm( new URI( url ) );
        }
        catch ( URISyntaxException e )
        {
            log.debug( "Invalid URL {}: {}", url, e.getMessage() );
            throw e;
        }

        if ( realmForUrl != null )
        {
            if ( realm.isAnonymous() && !realmForUrl.isAnonymous() )
            {
                log.debug( "Populated credentials for authentication realm {} from realm {}", realmId,
                           realmForUrl.getId() );
                realm.setUsername( realmForUrl.getUsername() );
                realm.setPassword( realmForUrl.getPassword() );
            }
        }

        log.debug( "Linked authentication URL {} to realm {}", url, realmId );
        realms.put( url, realm );
    }

    private String stripSlash( String s )
    {
        if ( s != null && s.length() > 0 )
        {
            int n = s.length() - 1;
            return s.charAt( n ) == '/' ? s.substring( 0, n ) : s;
        }
        return s;
    }

    public IAuthRealm getRealm( String realmId )
    {
        if ( realmId == null || realmId.trim().length() <= 0 )
        {
            return null;
        }
        realmId = normalizeRealmId( realmId );
        return realms.get( realmId );
    }

    public IAuthData select( URI uri )
    {
        uri.normalize();
        IAuthRealm realm = findRealm( uri );
        log.debug( "Selected authentication realm {} for URL {}", realm, uri );
        return ( realm != null && !realm.isAnonymous() ) ? realm.getAuthData() : null;
    }

    private IAuthRealm findRealm( URI uri )
    {
        String relativeUrl = "./";
        while ( true )
        {
            IAuthRealm realm = getRealm( uri.toString() );
            if ( realm != null )
            {
                return realm;
            }
            if ( uri.getPath().length() <= 1 )
            {
                break;
            }
            uri = uri.resolve( relativeUrl );
            relativeUrl = "../";
        }

        return null;
    }

    private void load()
    {
        if ( secureStorage == null )
        {
            log.warn( "Secure storage unavailable, not loading authentication registry" );
            return;
        }

        log.debug( "Loading authentication registry " );

        ISecurePreferences authNode = secureStorage.node( SECURE_NODE_PATH );

        realms.clear();

        for ( String nodeName : authNode.childrenNames() )
        {
            try
            {
                String realmId = decodeRealmId( nodeName );
                log.debug( "Loading authentication realm {}", realmId );
                ISecurePreferences realmNode = authNode.node( nodeName );

                IAuthRealm realm =
                    new AuthRealm( realmId, realmNode.get( SECURE_USERNAME, "" ), realmNode.get( SECURE_PASSWORD, "" ) );
                realms.put( realmId, realm );
            }
            catch ( StorageException e )
            {
                log.error( "Error loading authentication realm from node " + nodeName, e );
            }
        }

        for ( String realmKey : authNode.keys() )
        {
            if ( !realms.containsKey( realmKey ) )
            {
                try
                {
                    String nodeName = authNode.get( realmKey, "" );
                    if ( nodeName.length() > 0 )
                    {
                        String realmId = decodeRealmId( nodeName );
                        IAuthRealm realm = realms.get( realmId );
                        if ( realm != null )
                        {
                            log.debug( "Mapping {} to authentication realm {}", realmKey, realm );
                            realms.put( realmKey, realm );
                        }
                    }
                }
                catch ( StorageException e )
                {
                    log.error( "Error mapping authentication realm " + realmKey, e );
                }
            }
        }
    }

    private String encodeRealmId( String realmId )
        throws StorageException
    {
        realmId = normalizeRealmId( realmId );
        try
        {
            return EncodingUtils.encodeSlashes( EncodingUtils.encodeBase64( realmId.getBytes( "UTF-8" ) ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new StorageException( StorageException.INTERNAL_ERROR, e );
        }
    }

    private String decodeRealmId( String nodeName )
        throws StorageException
    {
        try
        {
            return new String( EncodingUtils.decodeBase64( EncodingUtils.decodeSlashes( nodeName ) ), "UTF-8" );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new StorageException( StorageException.INTERNAL_ERROR, e );
        }
    }

    private class AuthRealm
        implements IAuthRealm, IAuthData
    {
        private String id;

        private String username = "";

        private String password = "";

        private AuthRealm( String id )
        {
            this.id = id;
        }

        private AuthRealm( String id, String username, String password )
        {
            this.id = id;
            this.username = username;
            this.password = password;
        }

        public IAuthData getAuthData()
        {
            return this;
        }

        public String getId()
        {
            return id;
        }

        public void setUsername( String username )
        {
            if ( username == null )
            {
                username = "";
            }
            if ( !username.equals( this.username ) )
            {
                this.username = username;
                save();
            }
        }

        public String getUsername()
        {
            return username;
        }

        public void setPassword( String password )
        {
            if ( password == null )
            {
                password = "";
            }
            if ( !password.equals( this.password ) )
            {
                this.password = password;
                save();
            }
        }

        public String getPassword()
        {
            return password;
        }

        public boolean isAnonymous()
        {
            return username == null || username.trim().length() == 0;
        }

        private void save()
        {
            save( id );
        }

        private void save( String key )
        {
            if ( secureStorage == null )
            {
                log.debug( "Secure storage not available, can't save authentication registry." );
                return;
            }

            ISecurePreferences authNode = secureStorage.node( SECURE_NODE_PATH );

            try
            {
                String nodeName = encodeRealmId( id );
                authNode.put( key, nodeName, false );

                ISecurePreferences realmNode = authNode.node( nodeName );
                if ( !isAnonymous() )
                {
                    realmNode.put( SECURE_USERNAME, username, true );
                    realmNode.put( SECURE_PASSWORD, password, true );
                }
            }
            catch ( StorageException e )
            {
                log.error( "Error saving authentication realm " + id, e );
            }

            try
            {
                authNode.flush();
            }
            catch ( IOException e )
            {
                log.error( "Error saving authentication registry", e );
            }
        }

        @Override
        public String toString()
        {
            return getId();
        }
    }

    public IAuthRealm removeRealm( String realmId )
    {
        log.debug( "Removing authentication realm {}", realmId );

        realmId = normalizeRealmId( realmId );
        IAuthRealm realm = realms.remove( realmId );

        if ( secureStorage != null )
        {
            try
            {
                ISecurePreferences authNode = secureStorage.node( SECURE_NODE_PATH );
                authNode.remove( realmId );

                if ( realm != null )
                {
                    boolean used = false;
                    for ( IAuthRealm rlm : realms.values() )
                    {
                        if ( realm == rlm )
                        {
                            used = true;
                            break;
                        }
                    }
                    if ( !used )
                    {
                        String nodeName = encodeRealmId( realm.getId() );
                        ISecurePreferences realmNode = authNode.node( nodeName );
                        realmNode.removeNode();
                    }
                }

                authNode.flush();
            }
            catch ( StorageException e )
            {
                log.error( "Error removing authentication realm for " + realmId, e );
            }
            catch ( IOException e )
            {
                log.error( "Failed to flush secure storage", e );
            }
        }

        return realm;
    }

    public void clear()
    {
        log.debug( "Clearing authentication registry" );

        realms.clear();

        if ( secureStorage != null )
        {
            try
            {
                ISecurePreferences authNode = secureStorage.node( SECURE_NODE_PATH );
                authNode.removeNode();
                secureStorage.flush();
            }
            catch ( IOException e )
            {
                log.error( "Failed to flush secure storage", e );
            }
        }
    }

}
