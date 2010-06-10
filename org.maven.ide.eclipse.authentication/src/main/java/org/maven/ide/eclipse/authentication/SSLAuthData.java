package org.maven.ide.eclipse.authentication;

import java.io.File;

public class SSLAuthData
    implements ISSLAuthData
{
    private final File certificate;

    private final String passphrase;

    public SSLAuthData( File certificate, String passphrase )
    {
        this.certificate = certificate;
        this.passphrase = passphrase;
    }

    public File getCertificatePath()
    {
        return certificate;
    }

    public String getCertificatePassphrase()
    {
        return passphrase;
    }
}
