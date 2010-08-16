package org.maven.ide.eclipse.authentication.internal;

import java.io.File;

import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthRegistryException;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthData;

public class AuthData
    implements IAuthData
{
    private String username;

    private String password;

    private File certificatePath;

    private String certificatePassphrase;

    private AuthenticationType authenticationType;

    private AnonymousAccessType anonymousAccessType;

    public AuthData( String username, String password, AnonymousAccessType anonymousAccessType ) {
        this.username = username;
        this.password = password;
        this.anonymousAccessType = anonymousAccessType;
        authenticationType = AuthenticationType.USERNAME_PASSWORD;
    }

    public AuthData( File certificatePath, String certificatePassphrase ) {
        this.certificatePath = certificatePath;
        this.certificatePassphrase = certificatePassphrase;
        authenticationType = AuthenticationType.CERTIFICATE;
    }

    public AuthData( String username, String password, File certificatePath, String certificatePassphrase,
                     AnonymousAccessType anonymousAccessType ) {
        this.username = username;
        this.password = password;
        this.certificatePath = certificatePath;
        this.certificatePassphrase = certificatePassphrase;
        this.anonymousAccessType = anonymousAccessType;
        authenticationType = AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD;
    }

    public AuthData( AuthRealm realm, AnonymousAccessType anonymousAccessType ) {
        this.authenticationType = realm.getAuthenticationType();
        this.anonymousAccessType = anonymousAccessType;
        if ( allowsUsernameAndPassword() )
        {
            this.username = realm.getUsername();
            this.password = realm.getPassword();
        }
        if ( allowsCertificate() )
        {
            this.certificatePath = realm.getCertificatePath();
            this.certificatePassphrase = realm.getCertificatePassphrase();
        }
    }

    public AuthData() {
    }

    public AuthData( AuthenticationType authenticationType ) {
        this.authenticationType = authenticationType;
    }

    public AnonymousAccessType getAnonymousAccessType()
    {
        // TODO validate it matches authenticationType?
        return anonymousAccessType;
    }

    public void setAnonymousAccessType( AnonymousAccessType anonymousAccessType )
    {
        this.anonymousAccessType = anonymousAccessType;
    }

    public AuthenticationType getAuthenticationType()
    {
        return authenticationType;
    }

    public String getCertificatePassphrase()
    {
        // TODO validate it matches authenticationType?
        return certificatePassphrase;
    }

    public File getCertificatePath()
    {
        // TODO validate it matches authenticationType?
        return certificatePath;
    }

    public String getPassword()
    {
        // TODO validate it matches authenticationType?
        return password;
    }

    public String getUsername()
    {
        // TODO validate it matches authenticationType?
        return username;
    }

    public boolean allowsCertificate()
    {
        return AuthenticationType.CERTIFICATE.equals( authenticationType )
            || AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD.equals( authenticationType );
    }

    public boolean allowsUsernameAndPassword()
    {
        return authenticationType == null || AuthenticationType.USERNAME_PASSWORD.equals( authenticationType )
            || AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD.equals( authenticationType );
    }

    public void setUsernameAndPassword( String username, String password )
    {
        if ( !allowsUsernameAndPassword() )
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
        this.username = username;
        this.password = password;
    }

    public void setSSLCertificate( File certificatePath, String certificatePassphrase )
    {
        if ( !allowsCertificate() )
        {
            throw new AuthRegistryException(
                                             "The authentication type of this realm does not allow SSL certificate authentication." );
        }
        this.certificatePath = certificatePath;
        this.certificatePassphrase = certificatePassphrase;
    }

    public boolean isAnonymousAccessRequired()
    {
        return AnonymousAccessType.REQUIRED.equals( anonymousAccessType );
    }

    public boolean allowsAnonymousAccess()
    {
        return anonymousAccessType == null || AnonymousAccessType.ALLOWED.equals( anonymousAccessType )
            || AnonymousAccessType.REQUIRED.equals( anonymousAccessType );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( anonymousAccessType == null ) ? 0 : anonymousAccessType.hashCode() );
        result = prime * result + ( ( authenticationType == null ) ? 0 : authenticationType.hashCode() );
        if ( allowsCertificate() )
        {
            result = prime * result + ( ( certificatePassphrase == null ) ? 0 : certificatePassphrase.hashCode() );
            result = prime * result + ( ( certificatePath == null ) ? 0 : certificatePath.getAbsolutePath().hashCode() );
        }
        if ( allowsUsernameAndPassword() )
        {
            result = prime * result + ( ( password == null ) ? 0 : password.hashCode() );
            result = prime * result + ( ( username == null ) ? 0 : username.hashCode() );
        }
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        AuthData other = (AuthData) obj;
        if ( anonymousAccessType != other.anonymousAccessType )
        {
            return false;
        }
        if ( authenticationType != other.authenticationType )
        {
            return false;
        }
        if ( allowsCertificate() )
        {
            if ( certificatePassphrase == null )
            {
                if ( other.certificatePassphrase != null )
                {
                    return false;
                }
            }
            else if ( !certificatePassphrase.equals( other.certificatePassphrase ) )
            {
                return false;
            }
            if ( certificatePath == null )
            {
                if ( other.certificatePath != null )
                {
                    return false;
                }
            }
            else if ( !certificatePath.getAbsolutePath().equals( other.certificatePath.getAbsolutePath() ) )
            {
                return false;
            }
        }
        if ( allowsUsernameAndPassword() )
        {
            if ( password == null )
            {
                if ( other.password != null )
                {
                    return false;
                }
            }
            else if ( !password.equals( other.password ) )
            {
                return false;
            }
            if ( username == null )
            {
                if ( other.username != null )
                {
                    return false;
                }
            }
            else if ( !username.equals( other.username ) )
            {
                return false;
            }
        }
        return true;
    }
}
