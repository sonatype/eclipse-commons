package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.authentication.IAuthService;


public class UrlFetcher
{

    private FileFetcher fileFetcher = new FileFetcher();

    private HttpFetcher httpFetcher = new HttpFetcher();

    /**
     * Opens a stream to the specified resource.
     * 
     * @param url The resource to access, must not be {@code null}.
     * @param monitor The monitor to notify of transfer progress, may be {@code null}.
     * @param authService The authenticator service used to query credentials to access protected resources, may be
     *            {@code null}.
     * @param proxyService The proxy service used to select a proxy that is applicable for the resource, may be {@code
     *            null}.
     * @return The input stream to the specified resource, never {@code null}.
     * @throws IOException If the resource could not be opened.
     */
    public InputStream openStream( final URI url, final IProgressMonitor monitor, final IAuthService authService,
                                   final IProxyService proxyService )
        throws IOException
    {
        if ( isFile( url.getScheme() ) )
        {
            return fileFetcher.openStream( new File( url ), monitor );
        }
        else if ( isHttp( url.getScheme() ) )
        {
            return httpFetcher.openStream( url, monitor, authService, proxyService );
        }
        else
        {
            throw new IOException( "Unsupported protocol " + url.getScheme() );
        }
    }

    private static boolean isFile( String protocol )
    {
        return "file".equalsIgnoreCase( protocol );
    }

    private static boolean isHttp( String protocol )
    {
        return "http".equalsIgnoreCase( protocol ) || "https".equalsIgnoreCase( protocol );
    }

}
