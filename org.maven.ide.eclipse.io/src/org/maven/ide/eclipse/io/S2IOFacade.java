package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.auth.AuthFacade;
import org.maven.ide.eclipse.io.internal.S2IOPlugin;


public class S2IOFacade
{
    public static IProxyService getProxyService()
    {
        return S2IOPlugin.getDefault().getProxyService();
    }

    public static InputStream openStream( String uri, IProgressMonitor monitor )
        throws IOException, URISyntaxException
    {
        return new UrlFetcher().openStream( new URI( uri ), monitor, AuthFacade.getAuthService(),
                                            S2IOFacade.getProxyService() );
    }

    public static ServerResponse put( RequestEntity file, String uri, IProgressMonitor monitor )
        throws IOException, URISyntaxException
    {
        return put( file, uri, null /* default timeout */, monitor );
    }

    public static ServerResponse put( RequestEntity file, String uri, final Integer timeoutInMilliseconds,
                                      IProgressMonitor monitor, String monitorTaskName )
        throws IOException, URISyntaxException
    {
        UrlPublisher publisher = new UrlPublisher();
        publisher.setName( monitorTaskName );
        return publisher.putFile( file, new URI( uri ), monitor, AuthFacade.getAuthService(),
                                           S2IOFacade.getProxyService(), timeoutInMilliseconds );
    }

    public static ServerResponse put( RequestEntity file, String uri, final Integer timeoutInMilliseconds,
                                      IProgressMonitor monitor )
        throws IOException, URISyntaxException
    {
        return put( file, uri, timeoutInMilliseconds, monitor, null /* monitorTaskName */);
    }

    public static ServerResponse putFile( File file, String uri, IProgressMonitor monitor )
        throws IOException, URISyntaxException
    {
        return put( new FileRequestEntity( file ), uri, monitor );
    }
}
