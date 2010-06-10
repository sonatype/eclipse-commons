package org.maven.ide.eclipse.authentication;

import java.io.File;

public interface ISSLAuthData
{
    public File getCertificatePath();

    public String getCertificatePassphrase();
}
