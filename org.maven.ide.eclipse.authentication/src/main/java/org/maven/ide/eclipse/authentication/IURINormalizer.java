package org.maven.ide.eclipse.authentication;

import java.net.URISyntaxException;

public interface IURINormalizer
{
    String EXTENSION_POINT_ID = "org.maven.ide.eclipse.authentication.URINormalizer";

    boolean accept( String sUri )
        throws URISyntaxException;

    String normalize( String sUri )
        throws URISyntaxException;
}
