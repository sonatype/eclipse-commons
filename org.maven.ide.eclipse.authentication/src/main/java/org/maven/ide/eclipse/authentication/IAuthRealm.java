package org.maven.ide.eclipse.authentication;

import java.io.File;

public interface IAuthRealm
{
    public String getId();

    public String getName();

    public void setName( String name );

    public String getDescription();

    public void setDescription( String description );

    public void setUsernameAndPassword( String username, String password );

    public void setSSLCertificate( File sslCertificatePath, String sslCertificatePassphrase );

    IAuthData getAuthData();

    void setAuthData( IAuthData authData );

    public AuthenticationType getAuthenticationType();

    public void setAuthenticationType( AuthenticationType authenticationType );
}
