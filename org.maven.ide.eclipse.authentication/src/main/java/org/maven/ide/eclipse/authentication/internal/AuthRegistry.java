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

    private volatile ISecurityRealmPersistence persistence = null;

    private static volatile AuthRegistryStates state = AuthRegistryStates.NOT_LOADED;

    public static Object lock = new Integer( 0 );

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

    public IAuthRealm getRealm( String realmId )
    {
        if ( realmId == null || realmId.trim().length() <= 0 )
        {
            return null;
        }
        synchronized ( lock )
        {
            return realms.get( realmId );
        }
    }

    public IAuthData select( String sUri )
    {
        synchronized ( lock )
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
    }

    public IAuthData select( URI uri )
    {
        synchronized ( lock )
        {
            return select( uri.toString() );
        }
    }

    private ISecurityRealmURLAssoc findUrlAssoc( URI uri )
    {
        uri = uri.normalize();
        String relativeUrl = "./";
        while ( true )
        {
            String sURL = uri.toString();
            if ( sURL.endsWith( "/" ) )
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
        synchronized ( lock )
        {
            log.debug( "Loading security realms from persistent storage..." );

            loadCount++;

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
                    log.debug( "Found persisted URL to security realm association URL={} <--> realm id={}",
                               url.getUrl(), url.getRealmId() );

                    urlAssocsById.put( url.getId(), url );
                    urlAssocsByUrl.put( url.getUrl(), url );
                }
            }
            finally
            {
                state = AuthRegistryStates.LOADED;
                log.debug( "Loaded security realms from persistent storage in {} ms", System.currentTimeMillis()
                    - start );
            }
        }
    }

    // TODO Implement me
    public void removeRealm( String realmId, IProgressMonitor monitor )
    {
        synchronized ( lock )
        {
            log.debug( "Removing authentication realm {}", realmId );

            if ( realmId == null || realmId.trim().length() == 0 )
            {
                throw new AuthRegistryException( "The id of a security realm cannot be null or empty." );
            }
            if ( !realms.containsKey( realmId ) )
            {
                throw new AuthRegistryException( "A security realm with id='" + realmId + "' does not exists." );
            }

            if ( persistence != null )
            {
                persistence.deleteRealm( realmId, monitor );
            }

            realms.remove( realmId );
        }
    }

    public void clear()
    {
        synchronized ( lock )
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
    }

    public void reload( IProgressMonitor monitor )
    {
        synchronized ( lock )
        {
            log.debug( "Reloading AuthRegistry..." );

            realms.clear();
            urlAssocsById.clear();
            urlAssocsByUrl.clear();

            load( monitor );
        }
    }

    public IAuthRealm addRealm( String id, String name, String description, AuthenticationType authenticationType,
                                IProgressMonitor monitor )
    {
        synchronized ( lock )
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
            if ( persistence != null )
            {
                persistence.addRealm( newRealm, monitor );
            }
            ( (AuthRealm) newRealm ).loadFromSecureStorage( secureStorage );

            realms.put( id, newRealm );

            return newRealm;
        }
    }

    public ISecurityRealmURLAssoc addURLToRealmAssoc( String url, String realmId,
                                                      AnonymousAccessType anonymousAccessType, IProgressMonitor monitor )
    {
        log.debug( "Associating URL '{}' to realm id={}", url, realmId );

        synchronized ( lock )
        {
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
            if ( !realms.containsKey( realmId ) )
            {
                throw new AuthRegistryException( "A security realm with id='" + realmId + "' does not exist." );
            }

            url = url.trim();
            if ( url.endsWith( "/" ) )
            {
                url = url.substring( 0, url.length() - 1 );
            }
            url = URIHelper.normalize( url ).toString();

            if ( urlAssocsByUrl.containsKey( url ) )
            {
                throw new AuthRegistryException( "The '" + url + "' URL is already associated with a security realm." );
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
    }

    public void updateURLToRealmAssoc( ISecurityRealmURLAssoc urlToRealmAssoc, IProgressMonitor monitor )
    {
        synchronized ( lock )
        {
            if ( urlToRealmAssoc == null )
            {
                throw new AuthRegistryException( "The URL to realm association cannot be null." );
            }
            if ( urlToRealmAssoc.getUrl() == null || urlToRealmAssoc.getUrl().trim().length() == 0 )
            {
                throw new AuthRegistryException( "The url cannot be null or empty." );
            }
            if ( urlToRealmAssoc.getRealmId() == null || urlToRealmAssoc.getRealmId().trim().length() == 0 )
            {
                throw new AuthRegistryException( "The realm id cannot be null or empty." );
            }
            if ( urlToRealmAssoc.getAnonymousAccess() == null )
            {
                throw new AuthRegistryException( "The anonymousAccessType cannot be null." );
            }
            if ( !urlAssocsById.containsKey( urlToRealmAssoc.getId() ) )
            {
                throw new AuthRegistryException( "A URL to realm association with id='" + urlToRealmAssoc.getId()
                    + "' does not exist." );
            }
            if ( !realms.containsKey( urlToRealmAssoc.getRealmId() ) )
            {
                throw new AuthRegistryException( "A security realm with id='" + urlToRealmAssoc.getRealmId()
                    + "' does not exist." );
            }

            String url = urlToRealmAssoc.getUrl().trim();
            if ( url.endsWith( "/" ) )
            {
                url = url.substring( 0, url.length() - 1 );
            }
            url = URIHelper.normalize( url ).toString();
            urlToRealmAssoc.setUrl( url );

            ISecurityRealmURLAssoc otherAssoc = urlAssocsByUrl.get( url );
            if ( otherAssoc != null && !otherAssoc.getId().equals( urlToRealmAssoc.getId() ) )
            {
                throw new AuthRegistryException( "The '" + url + "' URL is already associated with a security realm." );
            }

            if ( persistence != null )
            {
                persistence.updateURLToRealmAssoc( urlToRealmAssoc, monitor );
            }

            urlAssocsById.put( urlToRealmAssoc.getId(), urlToRealmAssoc );
            for ( String urlKey : urlAssocsByUrl.keySet() )
            {
                ISecurityRealmURLAssoc someUrlAssoc = urlAssocsByUrl.get( urlKey );
                if ( someUrlAssoc.getId().equals( urlToRealmAssoc.getId() ) )
                {
                    urlAssocsByUrl.remove( urlKey );
                    break;
                }
            }
            urlAssocsByUrl.put( urlToRealmAssoc.getUrl(), urlToRealmAssoc );
        }
    }

    public void save( String uri, String username, String password )
    {
        synchronized ( lock )
        {
            IAuthData authData = new AuthData( username, password, AnonymousAccessType.NOT_ALLOWED );
            save( uri, authData );
        }
    }

    public void save( String uri, File certificatePath, String certificatePassphrase )
    {
        synchronized ( lock )
        {
            IAuthData authData = new AuthData( AuthenticationType.CERTIFICATE );
            authData.setSSLCertificate( certificatePath, certificatePassphrase );
            save( uri, authData );
        }
    }

    public void save( String sUri, IAuthData authData )
    {
        synchronized ( lock )
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
    }

    public IAuthRealm getRealmForURI( String sUri )
    {
        synchronized ( lock )
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

    public void updateRealm( IAuthRealm authRealm, IProgressMonitor monitor )
    {
        synchronized ( lock )
        {
            if ( authRealm == null )
            {
                throw new AuthRegistryException( "The security realm cannot be null." );
            }
            if ( authRealm.getId() == null || authRealm.getId().trim().length() == 0 )
            {
                throw new AuthRegistryException( "The id of a security realm cannot be null or empty." );
            }
            if ( authRealm.getName() == null || authRealm.getName().trim().length() == 0 )
            {
                throw new AuthRegistryException( "The name of a security realm cannot be null or empty." );
            }
            if ( !realms.containsKey( authRealm.getId() ) )
            {
                throw new AuthRegistryException( "A security realm with id='" + authRealm.getId() + "' does not exist." );
            }

            if ( persistence != null )
            {
                persistence.updateRealm( authRealm, monitor );
            }

            // Although it's not obvious, we have to load auth data from secure storage, not save it!
            // If the updated realm already has auth data, then that auth data was set through one of the methods that
            // saves
            // to secure storage, so we don't need to reload it because it is already up to date. But if the realm to
            // update
            // was constructed using one of the constructors, then it doesn't have auth data loaded, so we need to load
            // it
            // here.
            ( (AuthRealm) authRealm ).loadFromSecureStorage( secureStorage );

            realms.put( authRealm.getId(), authRealm );
        }
    }

    public ISecurityRealmURLAssoc getURLToRealmAssoc( String urlToRealmAssocId )
    {
        if ( urlToRealmAssocId == null || urlToRealmAssocId.trim().length() <= 0 )
        {
            return null;
        }
        synchronized ( lock )
        {
            return urlAssocsById.get( urlToRealmAssocId );
        }
    }

    public void removeURLToRealmAssoc( String urlToRealmAssocId, IProgressMonitor monitor )
    {
        synchronized ( lock )
        {
            log.debug( "Removing URL to realm association {}", urlToRealmAssocId );

            if ( urlToRealmAssocId == null || urlToRealmAssocId.trim().length() == 0 )
            {
                throw new AuthRegistryException( "The id of a URL to realm association cannot be null or empty." );
            }
            if ( !urlAssocsById.containsKey( urlToRealmAssocId ) )
            {
                throw new AuthRegistryException( "A URL to realm association with id='" + urlToRealmAssocId
                    + "' does not exists." );
            }

            if ( persistence != null )
            {
                persistence.deleteURLToRealmAssoc( urlToRealmAssocId, monitor );
            }

            ISecurityRealmURLAssoc urlAssoc = urlAssocsById.remove( urlToRealmAssocId );
            urlAssocsByUrl.remove( urlAssoc.getUrl() );
        }
    }

    private int loadCount = 0;

    public int getLoadCount()
    {
        return loadCount;
    }
}
