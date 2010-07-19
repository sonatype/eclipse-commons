package org.maven.ide.eclipse.authentication.internal;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.ISSLAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthRealm
    implements IAuthRealm, IAuthData, ISSLAuthData
{
    private final static Logger log = LoggerFactory.getLogger( AuthRealm.class );

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
     * Local filesystem path to ssl client certificate
     */
    private static final String SECURE_SSL_CERTIFICATE_PATH = "sslCertificatePath";

    /**
     * Passphrase to access ssl certificate
     */
    private static final String SECURE_SSL_CERTIFICATE_PASSPHRASE = "sslCertificatePassphrase";

    private final ISecurePreferences secureStorage;

    private String id;

    private String username = "";

    private String password = "";

    private File sslCertificate;

    private String sslCertificatePassphrase;

    private String name;

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
        this.sslCertificate = sslCertificate;
        this.sslCertificatePassphrase = sslCertificatePassphrase;
    }

    public AuthRealm( String id, String name, String description ) {
        this.secureStorage = null;
        this.id = id;
        this.name = name;
        this.description = description;
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
        if ( username != null && username.trim().length() != 0 )
        {
            return false;
        }

        if ( sslCertificate != null )
        {
            return false;
        }

        return true;
    }

    public ISSLAuthData getSSLAuthData()
    {
        return this;
    }

    public File getCertificatePath()
    {
        return sslCertificate;
    }

    public String getCertificatePassphrase()
    {
        return sslCertificatePassphrase;
    }

    public void setSSLAuthData( ISSLAuthData authData )
    {
        File oldCertificate = sslCertificate;
        String oldPassphrase = sslCertificatePassphrase;

        if ( authData == null )
        {
            sslCertificate = null;
            sslCertificatePassphrase = null;
        }
        else
        {
            sslCertificate = authData.getCertificatePath();
            sslCertificatePassphrase = authData.getCertificatePassphrase();
        }

        if ( !eq( oldCertificate, sslCertificate ) || !eq( oldPassphrase, sslCertificatePassphrase ) )
        {
            save();
        }
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

            String sslCertificatePath = sslCertificate != null ? sslCertificate.getCanonicalPath() : null;

            realmNode.put( SECURE_SSL_CERTIFICATE_PATH, sslCertificatePath, false );
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

    @Override
    public String toString()
    {
        return getId();
    }
}
