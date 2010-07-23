package org.maven.ide.eclipse.authentication.internal;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthRegistryException;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.IAuthRegistry;
import org.maven.ide.eclipse.authentication.IAuthService;
import org.maven.ide.eclipse.authentication.ISecurityRealmPersistence;
import org.maven.ide.eclipse.authentication.ISecurityRealmURLAssoc;
import org.maven.ide.eclipse.authentication.SecurityRealmURLAssoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthRegistry
    implements IAuthRegistry, IAuthService
{
    private final Logger log = LoggerFactory.getLogger( AuthRegistry.class );

    /**
     * Map of AuthRealm by realm id.
     */
    private Map<String, IAuthRealm> realms = new LinkedHashMap<String, IAuthRealm>();

    private Map<String, ISecurityRealmURLAssoc> urlAssocsById = new LinkedHashMap<String, ISecurityRealmURLAssoc>();

    private Map<String, ISecurityRealmURLAssoc> urlAssocsByUrl = new LinkedHashMap<String, ISecurityRealmURLAssoc>();

    private final ISecurePreferences secureStorage;

    private ISecurityRealmPersistence persistence = null;

    private static volatile AuthRegistryStates state = AuthRegistryStates.NOT_LOADED;

    public static AuthRegistryStates getState()
    {
        return state;
    }

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
        try
        {
            load( new NullProgressMonitor() );
        }
        catch ( Exception e )
        {
            log.error( "Error loading persisted realms: " + e.getMessage(), e );
            return;
        }
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
        return realms.get( realmId );
    }

    public IAuthData select( String sUri )
    {
        try
        {
            log.debug( "Finding auth data for URI {}.", sUri );
            URI uri = URIHelper.normalize( sUri );
            ISecurityRealmURLAssoc urlAssoc = findUrlAssoc( uri );
            if ( urlAssoc == null )
            {
                log.debug( "URI {} is not associated with a security realm.", uri );
                // Fall back to SimpleAuthService
                return new SimpleAuthService( secureStorage ).select( uri );
            }
            log.debug( "Selected authentication realm {} for URI {}", urlAssoc.getRealmId(), uri );
            IAuthRealm realm = realms.get( urlAssoc.getRealmId() );
            return new AuthData( (AuthRealm) realm, urlAssoc.getAnonymousAccess() );
        }
        catch ( Exception e )
        {
            log.error( "Error loading authentication for URI " + sUri, e );
        }
        return null;
    }

    public IAuthData select( URI uri )
    {
        return select( uri.toString() );
    }

    private ISecurityRealmURLAssoc findUrlAssoc( URI uri )
    {
        uri = uri.normalize();
        String relativeUrl = "./";
        while ( true )
        {
            String sURL = uri.toString();
            if (sURL.endsWith( "/" ))
            {
                sURL = sURL.substring( 0, sURL.length() - 1 );
            }
            ISecurityRealmURLAssoc urlAssoc = urlAssocsByUrl.get( sURL );
            if ( urlAssoc != null )
            {
                return urlAssoc;
            }
            if ( uri.getPath() == null || uri.getPath().length() <= 1 )
            {
                break;
            }
            uri = uri.resolve( relativeUrl );
            relativeUrl = "../";
        }

        return null;
    }

    private boolean securityRealmPersistenceWasLoaded = false;

    private void loadSecurityRealmPersistence()
    {
        if ( securityRealmPersistenceWasLoaded )
        {
            return;
        }
        securityRealmPersistenceWasLoaded = true;

        int selectedPriority = Integer.MAX_VALUE;
        ISecurityRealmPersistence selectedPersistence = null;
        
        IConfigurationElement[] persistenceExtensionConfigurationElements =
            Platform.getExtensionRegistry().getConfigurationElementsFor( ISecurityRealmPersistence.EXTENSION_POINT_ID );
        for ( IConfigurationElement persistenceExtensionConfigurationElement : persistenceExtensionConfigurationElements )
        {
            Object o;
            try
            {
                o = persistenceExtensionConfigurationElement.createExecutableExtension( "class" );
            }
            catch ( CoreException e )
            {
                log.error( "Error creating executable extension for security realm persistence: " + e.getMessage(), e );
                continue;
            }
            log.debug( "Found security realm persistence: {}", o.getClass().getCanonicalName() );
            if ( o instanceof ISecurityRealmPersistence )
            {
                ISecurityRealmPersistence persistence = (ISecurityRealmPersistence) o;
                int priority = persistence.getPriority();
                log.debug( "    with priority: {}", priority );
                if ( priority < selectedPriority )
                {
                    selectedPriority = priority;
                    selectedPersistence = persistence;
                }
            }
            else
            {
                throw new IllegalArgumentException( o.getClass().getCanonicalName()
                    + " does not implement the ISecurityRealmPersistence interface." );
            }
        }

        if ( selectedPersistence == null )
        {
            log.warn( "There is no implementation available for security realm persistence." );
        }
        else
        {
            log.info( "Using security realm persistence implementation: "
                + selectedPersistence.getClass().getCanonicalName() );
        }

        selectedPersistence.setActive( true );
        persistence = selectedPersistence;
    }

    private void load( IProgressMonitor monitor )
    {
        log.debug( "Loading security realms from persistent storage..." );

        long start = System.currentTimeMillis();
        state = AuthRegistryStates.LOADING;
        try
        {
            loadSecurityRealmPersistence();
            if ( persistence == null )
            {
                // Nothing to load
                return;
            }

            Set<IAuthRealm> persistedRealms = persistence.getRealms( monitor );
            log.debug( "Found {} persisted security realms.", persistedRealms.size() );
            for ( IAuthRealm realm : persistedRealms )
            {
                log.debug( "Found persisted security realm with id={}", realm.getId() );

                ( (AuthRealm) realm ).loadFromSecureStorage( secureStorage );
                realms.put( realm.getId(), realm );
            }

            Set<ISecurityRealmURLAssoc> persistedUrls = persistence.getURLToRealmAssocs( monitor );
            log.debug( "Found {} persisted URL to security realm associations.", persistedUrls.size() );
            for ( ISecurityRealmURLAssoc url : persistedUrls )
            {
                log.debug( "Found persisted URL to security realm association URL={} <--> realm id={}", url.getUrl(),
                           url.getRealmId() );

                urlAssocsById.put( url.getId(), url );
                urlAssocsByUrl.put( url.getUrl(), url );
            }
        }
        finally
        {
            state = AuthRegistryStates.LOADED;
            log.debug( "Loaded security realms from persistent storage in {} ms", System.currentTimeMillis() - start );
        }
    }

    // TODO Implement me
    public IAuthRealm removeRealm( String realmId )
    {
        log.debug( "Removing authentication realm {}", realmId );

        realmId = normalizeRealmId( realmId );
        IAuthRealm realm = realms.remove( realmId );

        // if ( secureStorage != null )
        // {
        // try
        // {
        // ISecurePreferences authNode = secureStorage.node( SECURE_NODE_PATH );
        // authNode.remove( realmId );
        //
        // if ( realm != null )
        // {
        // boolean used = false;
        // for ( IAuthRealm rlm : realms.values() )
        // {
        // if ( realm == rlm )
        // {
        // used = true;
        // break;
        // }
        // }
        // if ( !used )
        // {
        // String nodeName = encodeRealmId( realm.getId() );
        // ISecurePreferences realmNode = authNode.node( nodeName );
        // realmNode.removeNode();
        // }
        // }
        //
        // authNode.flush();
        // }
        // catch ( StorageException e )
        // {
        // log.error( "Error removing authentication realm for " + realmId, e );
        // }
        // catch ( IOException e )
        // {
        // log.error( "Failed to flush secure storage", e );
        // }
        // }

        return realm;
    }

    public void clear()
    {
        log.debug( "Clearing authentication registry" );

        realms.clear();
        urlAssocsById.clear();
        urlAssocsByUrl.clear();

        if ( persistence != null )
        {
            persistence.setActive( false );
            persistence = null;
        }
        securityRealmPersistenceWasLoaded = false;
    }

    public void reload( IProgressMonitor monitor )
    {
        log.debug( "Reloading AuthRegistry..." );

        realms.clear();
        urlAssocsById.clear();
        urlAssocsByUrl.clear();

        load( monitor );
    }

    public IAuthRealm addRealm( String id, String name, String description, AuthenticationType authenticationType, IProgressMonitor monitor )
    {
        if ( id == null || id.trim().length() == 0 )
        {
            throw new AuthRegistryException( "The id of a security realm cannot be null or empty." );
        }
        if ( name == null || name.trim().length() == 0 )
        {
            throw new AuthRegistryException( "The name of a security realm cannot be null or empty." );
        }
        if ( realms.containsKey( id ) )
        {
            throw new AuthRegistryException( "A security realm with id='" + id + "' already exists." );
        }
        
        IAuthRealm newRealm = new AuthRealm( id, name, description, authenticationType );
        if (persistence != null)
        {
            persistence.addRealm( newRealm, monitor );
        }

        realms.put( id, newRealm );

        return newRealm;
    }

    public ISecurityRealmURLAssoc addURLToRealmAssoc( String url, String realmId,
                                                      AnonymousAccessType anonymousAccessType, IProgressMonitor monitor )
    {
        log.debug( "Associating URL '{}' to realm id={}", url, realmId );

        if ( url == null || url.trim().length() == 0 )
        {
            throw new AuthRegistryException( "The url cannot be null or empty." );
        }
        if ( realmId == null || realmId.trim().length() == 0 )
        {
            throw new AuthRegistryException( "The realm id cannot be null or empty." );
        }
        if ( anonymousAccessType == null )
        {
            throw new AuthRegistryException( "The anonymousAccessType cannot be null." );
        }

        url = url.trim();
        if ( url.endsWith( "/" ) )
        {
            url = url.substring( 0, url.length() - 1 );
        }
        url = URIHelper.normalize( url ).toString();

        if ( urlAssocsByUrl.containsKey( url ) )
        {
            throw new AuthRegistryException( "The '{}' URL is already associated with a security realm." );
        }

        ISecurityRealmURLAssoc newUrlAssoc;
        if ( persistence != null )
        {
            newUrlAssoc = new SecurityRealmURLAssoc( null /* id */, url, realmId, anonymousAccessType );
            newUrlAssoc = persistence.addURLToRealmAssoc( newUrlAssoc, monitor );
        }
        else
        {
            newUrlAssoc = new SecurityRealmURLAssoc( url /* id */, url, realmId, anonymousAccessType );
        }

        urlAssocsById.put( newUrlAssoc.getId(), newUrlAssoc );
        urlAssocsByUrl.put( newUrlAssoc.getUrl(), newUrlAssoc );
        return newUrlAssoc;
    }

    public void save( String uri, String username, String password )
    {
        IAuthData authData = new AuthData( username, password, AnonymousAccessType.NOT_ALLOWED );
        save( uri, authData );
    }

    public void save( String uri, File certificatePath, String certificatePassphrase )
    {
        IAuthData authData = new AuthData( AuthenticationType.CERTIFICATE );
        authData.setSSLCertificate( certificatePath, certificatePassphrase );
        save( uri, authData );
    }

    public void save( String sUri, IAuthData authData )
    {
        URI uri = URIHelper.normalize( sUri );
        ISecurityRealmURLAssoc urlAssoc = findUrlAssoc( uri );
        if ( urlAssoc == null )
        {
            log.debug( "URL {} is not associated with a security realm.", uri );
            // Fall back to SimpleAuthService
            new SimpleAuthService( secureStorage ).save( uri.toString(), authData );
            return;
        }
        log.debug( "Selected authentication realm {} for URL {}", urlAssoc.getRealmId(), uri );
        IAuthRealm realm = realms.get( urlAssoc.getRealmId() );
        realm.setAuthData( authData );
    }

    public IAuthRealm getRealmForURI( String sUri )
    {
        log.debug( "Finding realm for URI {}.", sUri );
        URI uri = URIHelper.normalize( sUri );
        ISecurityRealmURLAssoc urlAssoc = findUrlAssoc( uri );
        if ( urlAssoc == null )
        {
            log.debug( "URI {} is not associated with a security realm.", uri );
            return null;
        }
        log.debug( "Selected authentication realm {} for URL {}", urlAssoc.getRealmId(), uri );
        return realms.get( urlAssoc.getRealmId() );
    }
}
