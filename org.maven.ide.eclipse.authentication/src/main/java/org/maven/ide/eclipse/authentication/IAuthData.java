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

    boolean allowsUsernameAndPassword();

    boolean allowsCertificate();

    public void setUsernameAndPassword( String username, String password );

    public void setSSLCertificate( File certificatePath, String certificatePassphrase );

    public boolean isAnonymousAccessRequired();
}
