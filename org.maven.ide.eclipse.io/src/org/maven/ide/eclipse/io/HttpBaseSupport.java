package org.maven.ide.eclipse.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.ProxyServer.Protocol;
import com.ning.http.client.Realm;

public class HttpBaseSupport
{

    private final Logger log = LoggerFactory.getLogger( HttpBaseSupport.class );

    private static final int DEFAULT_TIMEOUT = 30 * 1000;

    protected Realm realm = null;

    protected ProxyServer proxyServer = null;

    protected AsyncHttpClientConfig.Builder init( final URI url, final IAuthService authService,
                                                  final IProxyService proxyService, Integer timeoutInMilliseconds )
        throws IOException
    {
        IAuthData authData = null;
        if ( authService != null )
        {
            authData = authService.select( url );
        }
        return init( url, authData, proxyService, timeoutInMilliseconds );
    }

    protected AsyncHttpClientConfig.Builder init( final URI url, final IAuthData authData,
                                                  final IProxyService proxyService, Integer timeoutInMilliseconds )
        throws IOException
    {
        if ( timeoutInMilliseconds == null )
        {
            timeoutInMilliseconds = DEFAULT_TIMEOUT;
        }

        log.debug( "Using timeout={} ms", timeoutInMilliseconds );
        AsyncHttpClientConfig.Builder confBuilder =
            new AsyncHttpClientConfig.Builder().setRequestTimeoutInMs( timeoutInMilliseconds ).setCompressionEnabled( true );

        IProxyData proxy = selectProxy( url, proxyService );
        if ( proxy != null && proxy.getHost() != null )
        {
            int port = resolvePort( proxy.getPort(), proxy.getType() );
            Protocol protocol;
            if ( proxy.getType().equals( IProxyData.HTTP_PROXY_TYPE ) )
            {
                protocol = Protocol.HTTP;
            }
            else if ( proxy.getType().equals( IProxyData.HTTPS_PROXY_TYPE ) )
            {
                protocol = Protocol.HTTPS;
            }
            else if ( proxy.getType().equals( IProxyData.SOCKS_PROXY_TYPE ) )
            {
                // Not supported yet
                throw new RuntimeException( "SOCKS proxy not supported yet." );
            }
            else
            {
                throw new RuntimeException( "Unknown Proxy type: " + proxy.getType() );
            }

            if ( proxy.isRequiresAuthentication() )
            {
                proxyServer = new ProxyServer( protocol, proxy.getHost(), port, proxy.getUserId(), proxy.getPassword() );
                log.debug( "Connecting to {} via proxy {} and authentication", url, proxyServer.toString() );
            }
            else
            {
                proxyServer = new ProxyServer( protocol, proxy.getHost(), port );
                log.debug( "Connecting to {} via proxy {} and no authentication", url, proxyServer.toString() );
            }
        }
        else
        {
            log.debug( "Connecting to {} without proxy", url );
        }

        if ( authData != null )
        {
            if ( ( authData.getUsername() != null && authData.getUsername().length() > 0 )
                || ( authData.getPassword() != null && authData.getPassword().length() > 0 ) )
            {
                this.realm =
                    new Realm.RealmBuilder().setPassword( authData.getPassword() ).setUsePreemptiveAuth( true ).setPrincipal( authData.getUsername() ).build();
            }
        }
        // LogManager.setProvider( new Slf4jLoggerProvider() );
        
        return confBuilder;
    }

    public static com.ning.http.client.AsyncHandler.STATE handleStatus( HttpResponseStatus responseStatus )
    {
        return com.ning.http.client.AsyncHandler.STATE.CONTINUE;
    }
    
    public static Throwable getStatusException( String url, HttpResponseStatus responseStatus ) {
        int status = responseStatus.getStatusCode();
        if ( status != HttpURLConnection.HTTP_OK )
        {
            if ( HttpURLConnection.HTTP_UNAUTHORIZED == status )
            {
                return new UnauthorizedException( "HTTP status code " + status + ": "
                    + responseStatus.getStatusText() + ": " + url );
            }
            else if ( HttpURLConnection.HTTP_FORBIDDEN == status )
            {
                return new ForbiddenException( "HTTP status code " + status + ": "
                    + responseStatus.getStatusText() + ": " + url );
            }
            else if ( HttpURLConnection.HTTP_NOT_FOUND == status )
            {
                return new NotFoundException( "HTTP status code " + status + ": "
                    + responseStatus.getStatusText() + ": " + url );
            }
            else
            {
                return new IOException( "HTTP status code " + status + ": " + responseStatus.getStatusText()
                    + ": " + url );
            }
        }
        return null;
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
        else if ( IProxyData.HTTPS_PROXY_TYPE.equalsIgnoreCase( protocol ) )
        {
            return 443;
        }
        else
        {
            return 80;
        }
    }

    public static class HttpInputStream
        extends FilterInputStream
    {
        String encoding;
        private AsyncHttpClient client;

        public HttpInputStream( InputStream is, String encoding, AsyncHttpClient httpClient )
        {
            super( is );
            this.encoding = encoding;
            this.client = httpClient;
        }

        @Override
        public void close()
            throws IOException
        {
            try
            {
                if ( client != null )
                {
                    client.close();
                }
            }
            finally
            {
                super.close();
            }
        }

        public String getEncoding()
        {
            return encoding;
        }

    }

    protected abstract class BaseAsyncHandler
        implements AsyncHandler<String>
    {

        protected String encoding = null;

        public String getEncoding()
        {
            return encoding;
        }

        public void onThrowable( Throwable t )
        {

        }

        public STATE onBodyPartReceived( HttpResponseBodyPart bodyPart )
            throws Exception
        {
            return STATE.CONTINUE;
        }

        public STATE onStatusReceived( HttpResponseStatus responseStatus )
            throws Exception
        {
            return STATE.CONTINUE;
        }

        public STATE onHeadersReceived( HttpResponseHeaders headers )
            throws Exception
        {
            FluentCaseInsensitiveStringsMap h = headers.getHeaders();
            if ( h.containsKey( "Content-Type" ) )
            {
                String mime = h.getFirstValue( "Content-Type" ).toLowerCase();
                int i = mime.indexOf( "charset=" );
                if ( i > 0 )
                {
                    encoding = mime.substring( i + 8 ).trim();
                }
            }
            return STATE.CONTINUE;
        }

        public String onCompleted()
            throws Exception
        {
            return "";
        }
    }
}
