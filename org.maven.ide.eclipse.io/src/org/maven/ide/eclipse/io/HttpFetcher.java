package org.maven.ide.eclipse.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.security.B64Code;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.BufferUtil;
import org.eclipse.jetty.util.StringUtil;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpFetcher
    extends HttpBaseSupport
{

    private final Logger log = LoggerFactory.getLogger( HttpFetcher.class );

    private int redirects = 3;

    public HttpInputStream openStream( final URI url, final IProgressMonitor monitor, final IAuthService authService,
                                       final IProxyService proxyService )
        throws IOException
    {
        HttpClient httpClient = startClient( url, authService, proxyService, null /* default timeout */);

        PipedOutputStream os = new PipedOutputStream();
        MonitoredInputStream mis = new MonitoredInputStream( new PipedInputStream( os ), monitor );

        GetExchange exchange = new GetExchange( url.toString(), httpClient, os, mis );

        IAuthData authData = authService.select( url );
        if ( authData != null
            && ( ( authData.getUsername() != null && authData.getUsername().length() > 0 ) || ( authData.getPassword() != null && authData.getPassword().length() > 0 ) ) )
        {
            String authenticationString =
                "Basic "
                    + B64Code.encode( authData.getUsername() + ":" + authData.getPassword(), StringUtil.__ISO_8859_1 );
            exchange.setRequestHeader( HttpHeaders.AUTHORIZATION, authenticationString );
        }

        httpClient.send( exchange );

        return new HttpInputStream( mis, httpClient, exchange );
    }

    private void handleStatus( String url, int status, MonitoredInputStream mis )
    {
        if ( status != HttpStatus.OK_200 && mis != null )
        {
            if ( HttpStatus.UNAUTHORIZED_401 == status )
            {
                mis.setException( new UnauthorizedException( "HTTP status code " + status + ": "
                    + HttpStatus.getMessage( status ) + ": " + url ) );
            }
            else if ( HttpStatus.FORBIDDEN_403 == status )
            {
                mis.setException( new ForbiddenException( "HTTP status code " + status + ": "
                    + HttpStatus.getMessage( status ) + ": " + url ) );
            }
            else if ( HttpStatus.NOT_FOUND_404 == status )
            {
                mis.setException( new NotFoundException( "HTTP status code " + status + ": "
                    + HttpStatus.getMessage( status ) + ": " + url ) );
            }
            else
            {
                mis.setException( new IOException( "HTTP status code " + status + ": " + HttpStatus.getMessage( status )
                    + ": " + url ) );
            }
        }
    }

    class GetExchange
        extends DataExchange
    {

        private final String url;

        private final HttpClient httpClient;

        private MonitoredInputStream mis;

        private OutputStream os;

        private int redirected;

        public GetExchange( String url, HttpClient httpClient, OutputStream os, MonitoredInputStream mis )
        {
            this.url = url;
            this.httpClient = httpClient;
            this.os = os;
            this.mis = mis;

            mis.setName( "Reading URL " + url );

            setURL( url.toString() );
            addRequestHeader( "Pragma", "no-cache" );
            addRequestHeader( "Cache-Control", "no-cache, no-store" );
        }

        @Override
        protected void onResponseHeader( Buffer name, Buffer value )
            throws IOException
        {
            super.onResponseHeader( name, value );

            switch ( HttpHeaders.CACHE.getOrdinal( name ) )
            {
                case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                    if ( mis != null )
                    {
                        mis.setLength( BufferUtil.toInt( value ) );
                    }
                    break;

                case HttpHeaders.LOCATION_ORDINAL:
                    if ( isRedirected( getResponseStatus() ) && redirected < redirects )
                    {
                        String location = value.toString();

                        String url;
                        if ( location.indexOf( "://" ) > 0 )
                        {
                            url = location;
                        }
                        else
                        {
                            url = getScheme() + "://" + getAddress();
                            if ( !location.startsWith( "/" ) )
                            {
                                url += "/";
                            }
                            url += location;
                        }

                        GetExchange exchange = new GetExchange( url, httpClient, os, mis );
                        exchange.redirected = redirected + 1;

                        os = null;
                        mis = null;
                        setTimeoutTask( null );

                        httpClient.send( exchange );
                    }
                    break;

                case HttpHeaders.WWW_AUTHENTICATE_ORDINAL:
                    if ( isUnauthorized( getResponseStatus() ) )
                    {
                        String authType = scrapeAuthenticationType( value.toString() );
                        if ( "ntlm".equalsIgnoreCase( authType ) )
                        {
                            log.debug( "Detected NTLM, switching to JRE connection handling" );

                            ConnectionPumper pumper = new ConnectionPumper( url, httpClient, os, mis );

                            os = null;
                            mis = null;

                            new Thread( pumper ).start();
                        }
                    }
                    break;
            }
        }

        private boolean isRedirected( int status )
        {
            return status == HttpStatus.MOVED_PERMANENTLY_301 || status == HttpStatus.MOVED_TEMPORARILY_302;
        }

        private boolean isUnauthorized( int status )
        {
            return status == HttpStatus.UNAUTHORIZED_401;
        }

        private String scrapeAuthenticationType( String authString )
        {
            int idx = authString.indexOf( " " );
            return ( idx < 0 ? authString : authString.substring( 0, idx ) ).trim().toLowerCase( Locale.ENGLISH );
        }

        @Override
        protected void onResponseHeaderComplete()
            throws IOException
        {
            super.onResponseHeaderComplete();

            handleStatus( url, getResponseStatus(), mis );
        }

        @Override
        protected void onResponseContent( Buffer content )
            throws IOException
        {
            super.onResponseContent( content );

            if ( os != null )
            {
                try
                {
                    content.writeTo( os );
                }
                catch ( InterruptedIOException e )
                {
                    /*
                     * This happens naturally if the reader side closes an active connection and thereby shuts down the
                     * client which in turn stops the thread pool managing this writer thread.
                     */
                    if ( !httpClient.isRunning() )
                    {
                        return;
                    }
                    throw e;
                }
            }
        }

        @Override
        protected void onResponseComplete()
            throws IOException
        {
            super.onResponseComplete();

            close();
        }

        @Override
        protected void onExpire()
        {
            super.onExpire();

            error( new IOException( "Server did not respond within configured timeout interval" ) );
        }

        @Override
        protected void onException( Throwable e )
        {
            super.onException( e );

            error( e );
        }

        @Override
        protected void onConnectionFailed( Throwable e )
        {
            super.onConnectionFailed( e );

            error( e );
        }

        private void error( Throwable e )
        {
            if ( mis != null )
            {
                mis.setException( e );
            }

            close();
        }

        private void close()
        {
            if ( os != null )
            {
                try
                {
                    os.close();
                }
                catch ( IOException e )
                {
                    // tried our best
                }
            }
        }

    }

    class ConnectionPumper
        implements Runnable
    {

        private final String url;

        private OutputStream os;

        private MonitoredInputStream mis;

        private Proxy proxy;

        public ConnectionPumper( String url, HttpClient httpClient, OutputStream os, MonitoredInputStream mis )
        {
            this.url = url;
            this.os = os;
            this.mis = mis;

            if ( httpClient.getProxy() != null )
            {
                proxy = new Proxy( Proxy.Type.HTTP, httpClient.getProxy().toSocketAddress() );
            }
        }

        public void run()
        {
            try
            {
                HttpURLConnection connection;
                if ( proxy != null )
                {
                    connection = (HttpURLConnection) URI.create( url ).toURL().openConnection( proxy );
                }
                else
                {
                    connection = (HttpURLConnection) URI.create( url ).toURL().openConnection();
                }
                connection.setRequestProperty( "Pragma", "no-cache" );
                connection.setConnectTimeout( timeout );
                connection.setReadTimeout( timeout );

                InputStream is = connection.getInputStream();
                String contentEncoding = connection.getHeaderField( "Content-Encoding" );
                if ( "gzip".equalsIgnoreCase( contentEncoding ) )
                {
                    is = new GZIPInputStream( is );
                }

                mis.setLength( connection.getContentLength() );

                handleStatus( url, connection.getResponseCode(), mis );

                try
                {
                    byte[] buffer = new byte[1024 * 4];
                    int bytes;
                    while ( ( bytes = is.read( buffer ) ) >= 0 )
                    {
                        os.write( buffer, 0, bytes );
                    }
                }
                finally
                {
                    is.close();
                }
            }
            catch ( ThreadDeath e )
            {
                throw e;
            }
            catch ( Throwable e )
            {
                mis.setException( e );
            }
            finally
            {
                try
                {
                    os.close();
                }
                catch ( IOException ignored )
                {
                    // tried our best
                }
            }
        }

    }

}
