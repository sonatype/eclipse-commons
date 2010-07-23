package org.maven.ide.eclipse.authentication;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.maven.ide.eclipse.authentication.internal.AuthRealm;

public class AuthRealmTest
    extends TestCase
{
    public void testSetUsernameAndPassword_USERNAME_PASSWORD()
        throws CoreException
    {
        AuthRealm realm =
            new AuthRealm( "testSetUsernameAndPassword_USERNAME_PASSWORD", "realm-name", "realm-description",
                           AuthenticationType.USERNAME_PASSWORD );
        realm.setUsernameAndPassword( "user", "pass" );
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
        realm.setUsernameAndPassword( "user", "pass" );
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
            realm.setUsernameAndPassword( "user", "pass" );
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
        throws CoreException
    {
        AuthRealm realm =
            new AuthRealm( "testSetSSLCertificate_CERTIFICATE", "realm-name", "realm-description",
                           AuthenticationType.CERTIFICATE );
        realm.setSSLCertificate( new File( "sslp" ), "pp" );
        assertEquals( new File( "sslp" ), realm.getCertificatePath() );
        assertEquals( "pp", realm.getCertificatePassphrase() );
    }

    public void testSetSSLCertificate_CERTIFICATE_AND_USERNAME_PASSWORD()
        throws CoreException
    {
        AuthRealm realm =
            new AuthRealm( "testSetSSLCertificate_CERTIFICATE_AND_USERNAME_PASSWORD", "realm-name",
                           "realm-description", AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD );
        realm.setSSLCertificate( new File( "sslp" ), "pp" );
        assertEquals( new File( "sslp" ), realm.getCertificatePath() );
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
            realm.setSSLCertificate( new File( "sslp" ), "pp" );
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
