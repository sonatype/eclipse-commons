package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.authentication.IAuthService;


public class UrlPublisher
{
    private FilePublisher filePublisher = new FilePublisher();

    private HttpPublisher httpPublisher = new HttpPublisher();

    private String name;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Uploads a file to the specified URL.
     * 
     * @param file The file to upload, must not be {@code null}.
     * @param url The destination for the uploaded file, must not be {@code null}.
     * @param monitor The monitor to notify of transfer progress, may be {@code null}.
     * @param authService The authenticator service used to query credentials to access protected resources, may be
     *            {@code null}.
     * @param proxyService The proxy service used to select a proxy that is applicable for the resource, may be {@code
     *            null}.
     * @param timeoutInMilliseconds Timeout in milliseconds. If null, it will use the default timeout.
     * @return The server response, can be empty but never {@code null}.
     * @throws IOException If the resource could not be uploaded.
     * @throws TransferException If the server rejected the resource.
     */
    public ServerResponse putFile( final RequestEntity file, final URI url, final IProgressMonitor monitor,
                                   final IAuthService authService, final IProxyService proxyService,
                                   final Integer timeoutInMilliseconds )
        throws IOException
    {
        if ( isFile( url.getScheme() ) )
        {
            filePublisher.putFile( file, new File( url ), monitor );
            return new ServerResponse( 200, null, "UTF-8" );
        }
        else if ( isHttp( url.getScheme() ) )
        {
            return httpPublisher.putFile( file, url, monitor, name, authService, proxyService, timeoutInMilliseconds );
        }
        else
        {
            throw new IOException( "Unsupported protocol " + url.getScheme() );
        }
    }

    public ServerResponse delete( final URI url, final IProgressMonitor monitor, final IAuthService authService,
                                  final IProxyService proxyService, final Integer timeoutInMilliseconds )
        throws IOException
    {
        if ( isHttp( url.getScheme() ) )
        {
            return httpPublisher.delete( url, monitor, name, authService, proxyService, timeoutInMilliseconds );
        }
        throw new IOException( "Unsupported protocol " + url.getScheme() );
    }

    /**
     * Uploads a file to the specified URL.
     * 
     * @param file The file to upload, must not be {@code null}.
     * @param url The destination for the uploaded file, must not be {@code null}.
     * @param monitor The monitor to notify of transfer progress, may be {@code null}.
     * @param authService The authenticator service used to query credentials to access protected resources, may be
     *            {@code null}.
     * @param proxyService The proxy service used to select a proxy that is applicable for the resource, may be {@code
     *            null}.
     * @return The server response, can be empty but never {@code null}.
     * @throws IOException If the resource could not be uploaded.
     * @throws TransferException If the server rejected the resource.
     */
    public ServerResponse putFile( final RequestEntity file, final URI url, final IProgressMonitor monitor,
                                final IAuthService authService, final IProxyService proxyService )
        throws IOException
    {
        return putFile( file, url, monitor, authService, proxyService, null /* timeout */);
    }

    private static boolean isFile( String protocol )
    {
        return "file".equalsIgnoreCase( protocol );
    }

    private static boolean isHttp( String protocol )
    {
        return "http".equalsIgnoreCase( protocol ) || "https".equalsIgnoreCase( protocol );
    }

    public ServerResponse postFile( final RequestEntity file, final URI url, final IProgressMonitor monitor,
                                    final IAuthService authService, final IProxyService proxyService,
                                    final Integer timeoutInMilliseconds )
        throws IOException
    {
        if ( isFile( url.getScheme() ) )
        {
            filePublisher.putFile( file, new File( url ), monitor );
            return new ServerResponse( 200, null, "UTF-8" );
        }
        else if ( isHttp( url.getScheme() ) )
        {
            return httpPublisher.postFile( file, url, monitor, name, authService, proxyService, timeoutInMilliseconds );
        }
        else
        {
            throw new IOException( "Unsupported protocol " + url.getScheme() );
        }
    }
}
