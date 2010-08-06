package org.maven.ide.eclipse.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.security.B64Code;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.util.StringUtil;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpPublisher
    extends HttpBaseSupport
{
    private final Logger log = LoggerFactory.getLogger( HttpPublisher.class );

    /**
     * Uploads a file to the specified URL.
     * 
     * @param file The file to upload, must not be {@code null}.
     * @param url The destination for the uploaded file, must not be {@code null}.
     * @param monitor The monitor to notify of transfer progress, may be {@code null}.
     * @param monitorSubtaskName The text to be displayed by the monitor.
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
                                   String monitorSubtaskName, final IAuthService authService,
                                   final IProxyService proxyService, Integer timeoutInMilliseconds )
        throws IOException
    {
        PutExchange exchange = new PutExchange( url.toString() );
        return doDataExchange( exchange, file, url, monitor, monitorSubtaskName, authService, proxyService,
                               timeoutInMilliseconds );
    }

    public ServerResponse delete( final URI url, final IProgressMonitor monitor, String monitorSubtaskName,
                                  final IAuthService authService, final IProxyService proxyService,
                                  Integer timeoutInMilliseconds )
        throws IOException
    {
        DeleteExchange exchange = new DeleteExchange( url.toString() );
        return doDataExchange( exchange, null /* file */, url, monitor, monitorSubtaskName, authService, proxyService,
                               timeoutInMilliseconds );
    }

    private ServerResponse doDataExchange( final _DataExchange exchange, final RequestEntity file, final URI url,
                                           final IProgressMonitor monitor,
                                   String monitorSubtaskName, final IAuthService authService,
                                   final IProxyService proxyService, Integer timeoutInMilliseconds )
        throws IOException
    {
        if ( file != null )
        {
            InputStream is = file.getContent();

            MonitoredInputStream mis = new MonitoredInputStream( is, SubMonitor.convert( monitor ) );
            if ( monitorSubtaskName == null )
            {
                monitorSubtaskName = "Uploading file " + file.getName();
            }
            mis.setName( monitorSubtaskName );
            mis.setLength( (int) file.getContentLength() );

            exchange.setRequestHeader( HttpHeaders.CONTENT_LENGTH, Long.toString( file.getContentLength() ) );
            if ( file.getContentType() != null )
            {
                exchange.setRequestContentType( file.getContentType() );
            }

            exchange.setRequestContentSource( mis );
        }

        HttpClient httpClient = startClient( url, authService, proxyService, timeoutInMilliseconds );
        if ( authService != null )
        {
            IAuthData authData = authService.select( url );
            if ( authData != null )
            {
                String authenticationString =
                    "Basic "
                        + B64Code.encode( authData.getUsername() + ":" + authData.getPassword(),
                                          StringUtil.__ISO_8859_1 );
                exchange.setRequestHeader( HttpHeaders.AUTHORIZATION, authenticationString );
            }
        }
        httpClient.registerListener( "org.eclipse.jetty.client.webdav.WebdavListener" );
        httpClient.send( exchange );
        try
        {
            exchange.waitForDone( monitor, monitorSubtaskName );
        }
        catch ( InterruptedException e )
        {
            throw new IOException( "Transfer was interrupted" );
        }
        finally
        {
            try
            {
                httpClient.stop();
            }
            catch ( Exception e )
            {
                // ignore
            }
        }

        Throwable exception = exchange.getException();
        if ( exception != null )
        {
            throw (IOException) new IOException( exception.getMessage() ).initCause( exception );
        }
        
        ServerResponse response =
            new ServerResponse( exchange.getStatus(), exchange.getResponseContentBytes(), exchange.getEncoding() );

        int status = exchange.getResponseStatus();
        switch ( status )
        {
            case HttpStatus.OK_200:
            case HttpStatus.CREATED_201:
            case HttpStatus.ACCEPTED_202:
            case HttpStatus.NO_CONTENT_204:
                break;
            case HttpStatus.UNAUTHORIZED_401:
                throw new UnauthorizedException( "HTTP status code " + status + ": "
                    + HttpStatus.getMessage( status ) + ": " + url );
            case HttpStatus.FORBIDDEN_403:
                throw new ForbiddenException( "HTTP status code " + status + ": "
                    + HttpStatus.getMessage( status ) + ": " + url );
            case HttpStatus.NOT_FOUND_404:
                throw new NotFoundException( "HTTP status code " + status + ": "
                    + HttpStatus.getMessage( status ) + ": " + url );
            default:
                throw new TransferException( "HTTP status code " + status + ": " + HttpStatus.getMessage( status ) + ": "
                    + url, response, null );
        }

        return response;
    }

    private static class PutExchange
        extends _DataExchange
    {
        public PutExchange(String url)
        {
            super( url, HttpMethods.PUT );
        }
    }

    private static class PostExchange
        extends _DataExchange
    {
        public PostExchange( String url )
        {
            super( url, HttpMethods.POST );
        }
    }

    private static class DeleteExchange
        extends _DataExchange
    {
        public DeleteExchange( String url ) {
            super( url, HttpMethods.DELETE );
        }
    }

    private abstract static class _DataExchange
        extends DataExchange
    {

        private Throwable exception;

        private ByteArrayOutputStream baos = new ByteArrayOutputStream( 1024 );

        public _DataExchange( String url, String httpMethod )
        {
            setURL( url );
            setMethod( httpMethod );
        }

        public Throwable getException()
        {
            return exception;
        }

        public int waitForDone( IProgressMonitor monitor, String monitorTaskName )
            throws InterruptedException
        {
            if ( monitor == null )
            {
                return waitForDone();
            }
            synchronized ( this )
            {
                boolean monitorStarted = false;
                int totalWork = 100;
                int worked = 0;
                IProgressMonitor subMonitor = null;
                while ( !isDone( getStatus() ) )
                {
                    this.wait( 100 );
                    if ( getStatus() == HttpExchange.STATUS_WAITING_FOR_RESPONSE )
                    {
                        if ( !monitorStarted )
                        {
                            worked = 0;
                            monitorStarted = true;
                            subMonitor = SubMonitor.convert( monitor, monitorTaskName, totalWork );
                        }
                        worked++;
                        subMonitor.worked( 1 );
                        if ( worked == totalWork )
                        {
                            // Force the monitor progress to restart
                            monitorStarted = false;
                        }
                    }
                }
            }
            return getStatus();
        }

        @Override
        protected void onConnectionFailed( Throwable x )
        {
            super.onConnectionFailed( x );

            error( x );
        }

        @Override
        protected void onException( Throwable x )
        {
            super.onException( x );

            error( x );
        }

        @Override
        protected void onExpire()
        {
            super.onExpire();

            error( new IOException( "The server did not respond within the configured timeout" ) );
        }

        @Override
        protected void onResponseContent( Buffer content )
            throws IOException
        {
            super.onResponseContent( content );

            content.writeTo( baos );
        }

        @Override
        public byte[] getResponseContentBytes()
        {
            return baos.toByteArray();
        }

        private void error( Throwable e )
        {
            exception = e;
        }
    }

    public ServerResponse postFile( final RequestEntity file, final URI url, final IProgressMonitor monitor,
                                    String monitorSubtaskName, final IAuthService authService,
                                    final IProxyService proxyService, Integer timeoutInMilliseconds )
        throws IOException
    {
        PostExchange exchange = new PostExchange( url.toString() );
        return doDataExchange( exchange, file, url, monitor, monitorSubtaskName, authService, proxyService,
                               timeoutInMilliseconds );
    }
}
