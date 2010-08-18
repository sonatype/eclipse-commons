package org.maven.ide.eclipse.authentication;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.authentication.internal.AuthRealm;

public class AuthRealmTest
    extends TestCase
{
    private static void setRealmUsernameAndPassword( IAuthRealm realm, AuthenticationType authType, String username,
                                                     String password )
    {
        IAuthData authData = new AuthData( authType );
        authData.setUsernameAndPassword( username, password );
        realm.setAuthData( authData );
    }

    private static void setRealmSSLCertificate( IAuthRealm realm, AuthenticationType authType, File certificatePath,
                                                String passphrase )
    {
        IAuthData authData = new AuthData( authType );
        authData.setSSLCertificate( certificatePath, passphrase );
        realm.setAuthData( authData );
    }

    public void testSetUsernameAndPassword_USERNAME_PASSWORD()
        throws CoreException
    {
        AuthRealm realm =
            new AuthRealm( "testSetUsernameAndPassword_USERNAME_PASSWORD", "realm-name", "realm-description",
                           AuthenticationType.USERNAME_PASSWORD );
        setRealmUsernameAndPassword( realm, AuthenticationType.USERNAME_PASSWORD, "user", "pass" );
        assertEquals( "user", realm.getUsername() );
        assertEquals( "pass", realm.getPassword() );
    }

    public void testSetUsernameAndPassword_CERTIFICATE_AND_USERNAME_PASSWORD()
        throws CoreException
    {
        AuthRealm realm =
            new AuthRealm( "testSetUsernameAndPassword_CERTIFICATE_AND_USERNAME_PASSWORD", "realm-name",
                           "realm-description",
                           AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD );
        setRealmUsernameAndPassword( realm, AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD, "user", "pass" );
        assertEquals( "user", realm.getUsername() );
        assertEquals( "pass", realm.getPassword() );
    }

    public void testSetUsernameAndPassword_CERTIFICATE()
        throws CoreException
    {
        IAuthRealm realm =
            new AuthRealm( "testSetUsernameAndPassword_CERTIFICATE", "realm-name", "realm-description",
                           AuthenticationType.CERTIFICATE );
        try
        {
            setRealmUsernameAndPassword( realm, AuthenticationType.CERTIFICATE, "user", "pass" );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals(
                                                "The authentication type of this realm does not allow username and password authentication." ) )
            {
                throw expected;
            }
        }
    }

    public void testSetSSLCertificate_CERTIFICATE()
        throws Exception
    {
        AuthRealm realm =
            new AuthRealm( "testSetSSLCertificate_CERTIFICATE", "realm-name", "realm-description",
                           AuthenticationType.CERTIFICATE );
        setRealmSSLCertificate( realm, AuthenticationType.CERTIFICATE, new File( "sslp" ), "pp" );
        assertEquals( new File( "sslp" ).getCanonicalFile(), realm.getCertificatePath() );
        assertEquals( "pp", realm.getCertificatePassphrase() );
    }

    public void testSetSSLCertificate_CERTIFICATE_AND_USERNAME_PASSWORD()
        throws Exception
    {
        AuthRealm realm =
            new AuthRealm( "testSetSSLCertificate_CERTIFICATE_AND_USERNAME_PASSWORD", "realm-name",
                           "realm-description", AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD );
        setRealmSSLCertificate( realm, AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD, new File( "sslp" ), "pp" );
        assertEquals( new File( "sslp" ).getCanonicalFile(), realm.getCertificatePath() );
        assertEquals( "pp", realm.getCertificatePassphrase() );
    }

    public void testSetSSLCertificate_USERNAME_PASSWORD()
        throws CoreException
    {
        IAuthRealm realm =
            new AuthRealm( "testSetSSLCertificate_USERNAME_PASSWORD", "realm-name", "realm-description",
                           AuthenticationType.USERNAME_PASSWORD );
        try
        {
            setRealmSSLCertificate( realm, AuthenticationType.USERNAME_PASSWORD, new File( "sslp" ), "pp" );
            fail( "Expected AuthRegistryException" );
        }
        catch ( AuthRegistryException expected )
        {
            if ( !expected.getMessage().equals(
                                                "The authentication type of this realm does not allow SSL certificate authentication." ) )
            {
                throw expected;
            }
        }
    }
}
