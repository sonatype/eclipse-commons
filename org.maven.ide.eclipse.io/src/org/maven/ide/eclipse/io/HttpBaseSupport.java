package org.maven.ide.eclipse.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.security.ProxyAuthorization;
import org.eclipse.jetty.client.security.Realm;
import org.eclipse.jetty.client.security.RealmResolver;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.http.security.B64Code;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.thread.Timeout.Task;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpBaseSupport
{

    private final Logger log = LoggerFactory.getLogger( HttpBaseSupport.class );

    protected int timeout = 10 * 1000;

    protected HttpClient startClient( final URI url, final IAuthService authService, final IProxyService proxyService,
                                      Integer timeoutInMilliseconds )
        throws IOException
    {
        IAuthData authData = null;
        if ( authService != null )
        {
            authData = authService.select( url );
        }
        return startClient( url, authData, proxyService, timeoutInMilliseconds );
    }

    protected HttpClient startClient( final URI url, final IAuthData authData, final IProxyService proxyService,
                                      Integer timeoutInMilliseconds )
        throws IOException
    {
        HttpClient httpClient = new FixedHttpClient();
        httpClient.setConnectorType( HttpClient.CONNECTOR_SELECT_CHANNEL );
        //httpClient.setConnectorType( HttpClient.CONNECTOR_SOCKET );
        if ( timeoutInMilliseconds == null )
        {
            timeoutInMilliseconds = timeout;
        }
        httpClient.setTimeout( timeoutInMilliseconds );
        // httpClient.setConnectTimeout( timeout );
        httpClient.setMaxRetries( 1 );

        IProxyData proxy = selectProxy( url, proxyService );
        if ( proxy != null && proxy.getHost() != null )
        {
            httpClient.setProxy( new Address( proxy.getHost(), resolvePort( proxy.getPort(), proxy.getType() ) ) );

            if ( proxy.isRequiresAuthentication() )
            {
                httpClient.setProxyAuthentication( new ProxyAuthorization( proxy.getUserId(), proxy.getPassword() ) );

                log.debug( "Connecting to {} via proxy {} and authentication", url, httpClient.getProxy() );
            }
            else
            {
                log.debug( "Connecting to {} via proxy {} and no authentication", url, httpClient.getProxy() );
            }
        }
        else
        {
            log.debug( "Connecting to {} without proxy", url );
        }

        httpClient.setRealmResolver( new RealmResolver()
        {
            public Realm getRealm( final String realmName, final HttpDestination destination, final String path )
                throws IOException
            {
                if ( authData != null )
                {
                    return new SimpleRealm( realmName, authData );
                }

                return null;
            }
        } );

        try
        {
            httpClient.start();
        }
        catch ( Exception e )
        {
            throw (IOException) new IOException( "Failed to initialize HTTP client: " + e.getMessage() ).initCause( e );
        }

        return httpClient;
    }

    private IProxyData selectProxy( URI url, IProxyService proxyService )
    {
        if ( proxyService != null && proxyService.isProxiesEnabled() )
        {
            IProxyData[] proxies = proxyService.select( url );

            if ( proxies.length > 0 )
            {
                if ( proxies.length == 1 )
                {
                    return proxies[0];
                }
                else
                {
                    String protocol = url.getScheme();
                    for ( IProxyData proxy : proxies )
                    {
                        if ( protocol.equalsIgnoreCase( proxy.getType() ) )
                        {
                            return proxy;
                        }
                    }
                    return proxies[0];
                }
            }
        }

        return null;
    }

    private int resolvePort( int port, String protocol )
    {
        if ( port >= 0 )
        {
            return port;
        }
        else if ( HttpSchemes.HTTPS.equalsIgnoreCase( protocol ) )
        {
            return 443;
        }
        else
        {
            return 80;
        }
    }

    protected static void setAuthenticationHeader( IAuthData authData, HttpExchange exchange )
        throws UnsupportedEncodingException
    {
        if ( authData != null
            && ( ( authData.getUsername() != null && authData.getUsername().length() > 0 ) || ( authData.getPassword() != null && authData.getPassword().length() > 0 ) ) )
        {
            String authenticationString =
                "Basic "
                    + B64Code.encode( authData.getUsername() + ":" + authData.getPassword(), StringUtil.__ISO_8859_1 );
            exchange.setRequestHeader( HttpHeaders.AUTHORIZATION, authenticationString );
        }
    }

    static class SimpleRealm
        implements Realm
    {

        private final String realmName;

        private final IAuthData auth;

        public SimpleRealm( final String realmName, final IAuthData auth )
        {
            this.realmName = realmName;
            this.auth = auth;
        }

        public String getPrincipal()
        {
            return auth.getUsername();
        }

        public String getCredentials()
        {
            return auth.getPassword();
        }

        public String getId()
        {
            return realmName;
        }

    }

    static class FixedHttpClient
        extends HttpClient
    {

        DataExchange httpExchange;

        @Override
        public void send( HttpExchange exchange )
            throws IOException
        {
            if ( exchange instanceof DataExchange )
            {
                httpExchange = (DataExchange) exchange;
            }

            super.send( exchange );
        }

        @Override
        public void schedule( Task task )
        {
            super.schedule( task );

            // hack/workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=296650
            if ( httpExchange != null )
            {
                httpExchange.setTimeoutTask( task );
            }
        }

    }

    static class DataExchange
        extends ContentExchange
    {

        private Task timeoutTask;

        private String encoding;

        public void setTimeoutTask( Task timeoutTask )
        {
            this.timeoutTask = timeoutTask;
        }

        @Override
        protected void onResponseContent( Buffer content )
            throws IOException
        {
            // hack/workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=296650
            if ( timeoutTask != null )
            {
                timeoutTask.reschedule();
            }
        }

        @Override
        public Buffer getRequestContentChunk()
            throws IOException
        {
            // hack/workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=296650
            if ( timeoutTask != null )
            {
                timeoutTask.reschedule();
            }

            return super.getRequestContentChunk();
        }

        @Override
        protected void onResponseHeader( Buffer name, Buffer value )
            throws IOException
        {
            super.onResponseHeader( name, value );

            int header = HttpHeaders.CACHE.getOrdinal( name );
            switch ( header )
            {
                case HttpHeaders.CONTENT_TYPE_ORDINAL:
                    String mime = StringUtil.asciiToLowerCase( value.toString() );
                    int i = mime.indexOf( "charset=" );
                    if ( i > 0 )
                    {
                        encoding = mime.substring( i + 8 ).trim();
                    }
                    break;
            }
        }

        public String getEncoding()
        {
            return encoding;
        }
    }

    public static class HttpInputStream
        extends FilterInputStream
    {

        private final HttpClient httpClient;

        private final DataExchange exchange;

        public HttpInputStream( InputStream is, HttpClient httpClient, DataExchange exchange )
        {
            super( is );

            this.httpClient = httpClient;
            this.exchange = exchange;
        }

        @Override
        public void close()
            throws IOException
        {
            super.close();

            try
            {
                httpClient.stop();
            }
            catch ( Exception e )
            {
                throw (IOException) new IOException().initCause( e );
            }
        }

        public String getEncoding()
        {
            return exchange.getEncoding();
        }
        
    }

}
