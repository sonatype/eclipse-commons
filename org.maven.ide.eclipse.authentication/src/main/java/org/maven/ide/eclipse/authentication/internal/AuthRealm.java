package org.maven.ide.eclipse.authentication.internal;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.maven.ide.eclipse.authentication.AuthRegistryException;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthRealm
    implements IAuthRealm
{
    private final static Logger log = LoggerFactory.getLogger( AuthRealm.class );

    /**
     * The path to the root node for the auth registry. Each realm will be saved in a child of this node.
     */
    private static final String SECURE_NODE_PATH = "org.maven.ide.eclipse.authentication.registry";

    /**
     * The key to save the username into.
     */
    private static final String SECURE_USERNAME = "username";

    /**
     * The key to save the password into.
     */
    private static final String SECURE_PASSWORD = "password";

    /**
     * Local filesystem path to ssl client certificate
     */
    private static final String SECURE_SSL_CERTIFICATE_PATH = "sslCertificatePath";

    /**
     * Passphrase to access ssl certificate
     */
    private static final String SECURE_SSL_CERTIFICATE_PASSPHRASE = "sslCertificatePassphrase";

    private ISecurePreferences secureStorage;

    private String id;

    private String username = "";

    private String password = "";

    private File sslCertificatePath;

    private String sslCertificatePassphrase;

    private String name;

    private AuthenticationType authenticationType = AuthenticationType.USERNAME_PASSWORD;

    public AuthenticationType getAuthenticationType()
    {
        return authenticationType;
    }

    public void setAuthenticationType( AuthenticationType authenticationType )
    {
        this.authenticationType = authenticationType;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    private String description;

    public AuthRealm( ISecurePreferences secureStorage, String id ) {
        this.secureStorage = secureStorage;
        this.id = id;
    }

    AuthRealm( ISecurePreferences secureStorage, String id, String username, String password, File sslCertificate,
               String sslCertificatePassphrase )
{
        this.secureStorage = secureStorage;
        this.id = id;
        this.username = username;
        this.password = password;
        this.sslCertificatePath = sslCertificate;
        this.sslCertificatePassphrase = sslCertificatePassphrase;
    }

    public AuthRealm( String id, String name, String description, AuthenticationType authenticationType ) {
        this.secureStorage = null;
        this.id = id;
        this.name = name;
        this.description = description;
        this.authenticationType = authenticationType;
    }

    public String getId()
    {
        return id;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public boolean isAnonymous()
    {
        if ( username != null && username.trim().length() != 0 )
        {
            return false;
        }

        if ( sslCertificatePath != null )
        {
            return false;
        }

        return true;
    }

    public File getCertificatePath()
    {
        return sslCertificatePath;
    }

    public String getCertificatePassphrase()
    {
        return sslCertificatePassphrase;
    }

    private static <T> boolean eq( T a, T b )
    {
        return a != null ? a.equals( b ) : b == null;
    }

    private void save()
    {
        save( id );
    }

    private String encodeRealmId( String realmId )
        throws StorageException
    {
        // realmId = normalizeRealmId( realmId );
        try
        {
            return EncodingUtils.encodeSlashes( EncodingUtils.encodeBase64( realmId.getBytes( "UTF-8" ) ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new StorageException( StorageException.INTERNAL_ERROR, e );
        }
    }

    void save( String key )
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

            realmNode.put( SECURE_USERNAME, username, true );
            realmNode.put( SECURE_PASSWORD, password, true );

            String sslCertificatePathString = sslCertificatePath != null ? sslCertificatePath.getCanonicalPath() : null;
            realmNode.put( SECURE_SSL_CERTIFICATE_PATH, sslCertificatePathString, false );
            realmNode.put( SECURE_SSL_CERTIFICATE_PASSPHRASE, sslCertificatePassphrase, true );
        }
        catch ( Exception e )
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

    void loadFromSecureStorage( ISecurePreferences secureStorage )
    {
        try
        {
            log.debug( "Loading authentication realm {}", id );

            this.secureStorage = secureStorage;

            ISecurePreferences authNode = secureStorage.node( SECURE_NODE_PATH );
            ISecurePreferences realmNode = authNode.node( encodeRealmId( id ) );

            username = realmNode.get( SECURE_USERNAME, "" );
            password = realmNode.get( SECURE_PASSWORD, "" );

            String sslCertificatePathString = realmNode.get( SECURE_SSL_CERTIFICATE_PATH, null );
            sslCertificatePath = sslCertificatePathString != null ? new File( sslCertificatePathString ) : null;
            sslCertificatePassphrase = realmNode.get( SECURE_SSL_CERTIFICATE_PASSPHRASE, null );
        }
        catch ( StorageException e )
        {
            log.error( "Error loading authentication realm from node " + id, e );
        }
    }

    @Override
    public String toString()
    {
        return getId();
    }

    public void setUsernameAndPassword( String username, String password )
    {
        if ( !AuthenticationType.USERNAME_PASSWORD.equals( authenticationType )
            && !AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD.equals( authenticationType ) )
        {
            throw new AuthRegistryException(
                                             "The authentication type of this realm does not allow username and password authentication." );
        }
        if ( username == null )
        {
            username = "";
        }
        if ( password == null )
        {
            password = "";
        }
        if ( !username.equals( this.username ) || !password.equals( this.password ) )
        {
            this.username = username;
            this.password = password;
            save();
        }
    }

    public void setSSLCertificate( File sslCertificatePath, String sslCertificatePassphrase )
    {
        if ( !AuthenticationType.CERTIFICATE.equals( authenticationType )
            && !AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD.equals( authenticationType ) )
        {
            throw new AuthRegistryException(
                                             "The authentication type of this realm does not allow SSL certificate authentication." );
        }
        if ( !sslCertificatePath.equals( this.sslCertificatePath )
            || !sslCertificatePassphrase.equals( this.sslCertificatePassphrase ) )
        {
            this.sslCertificatePath = sslCertificatePath;
            this.sslCertificatePassphrase = sslCertificatePassphrase;
            save();
        }
    }

    public void setAuthData( IAuthData authData )
    {
        if ( authData.getAuthenticationType() != null && !authData.getAuthenticationType().equals( authenticationType ) )
        {
            throw new AuthRegistryException( "Security realm " + id
                + ": The authentication type of the realm " + authenticationType
                + " does not match the authentication type of the provide authentication data "
                + authData.getAuthenticationType() );
        }
        if ( AuthenticationType.USERNAME_PASSWORD.equals( authenticationType )
            || AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD.equals( authenticationType ) )
        {
            setUsernameAndPassword( authData.getUsername(), authData.getPassword() );
        }
        if ( AuthenticationType.CERTIFICATE.equals( authenticationType )
            || AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD.equals( authenticationType ) )
        {
            setSSLCertificate( authData.getCertificatePath(), authData.getCertificatePassphrase() );
        }
    }

    public IAuthData getAuthData()
    {
        IAuthData authData = new AuthData( authenticationType );
        if ( authData.allowsUsernameAndPassword() )
        {
            authData.setUsernameAndPassword( username, password );
        }
        if ( authData.allowsCertificate() )
        {
            authData.setSSLCertificate( sslCertificatePath, sslCertificatePassphrase );
        }
        return authData;
    }
}
