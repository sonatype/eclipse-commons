package org.maven.ide.eclipse.authentication;

import java.io.File;

public interface IAuthData
{
    String getUsername();

    String getPassword();

    File getCertificatePath();

    String getCertificatePassphrase();

    AuthenticationType getAuthenticationType();

    AnonymousAccessType getAnonymousAccessType();

    public boolean allowsUsernameAndPassword();

    public boolean allowsCertificate();

    public boolean allowsAnonymousAccess();

    public void setUsernameAndPassword( String username, String password );

    public void setSSLCertificate( File certificatePath, String certificatePassphrase );

    public boolean isAnonymousAccessRequired();
}
