package org.maven.ide.eclipse.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.authentication.IAuthService;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

public class HttpFetcher
    extends HttpBaseSupport
{

    private HttpInputStream his = null;

	public HttpInputStream openStream( final URI url, final IProgressMonitor monitor, final IAuthService authService,
                                       final IProxyService proxyService )
        throws IOException
    {

        boolean followRedirects = true;
        int redirects = 3;

        AsyncHttpClientConfig.Builder confBuilder = init( url, authService, proxyService, null );

        AsyncHttpClientConfig conf =
            confBuilder.setFollowRedirects( followRedirects ).setMaximumNumberOfRedirects( redirects ).build();

        AsyncHttpClient httpClient = new AsyncHttpClient( conf );

        PipedInputStream pis = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(pis);
        final MonitoredInputStream mis = new MonitoredInputStream( pis, monitor );

        FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
        headers.add( "Pragma", "no-cache" );
        headers.add( "Cache-Control", "no-cache, no-store" );

        BoundRequestBuilder requestBuilder =
            httpClient.prepareGet( url.toString() ).setRealm( realm ).setHeaders( headers ).setProxyServer( proxyServer );

        his = new HttpInputStream(mis, "UTF-8");
        
        requestBuilder.execute(new GetAsyncHandler(pos, mis, url));
        
        return his;
    }

    private final class GetAsyncHandler
        extends BaseAsyncHandler
    {
        private final MonitoredInputStream mis;

        private final OutputStream os;

        private final URI url;

        private GetAsyncHandler( OutputStream os, MonitoredInputStream mis, URI url )
        {
            this.os = os;
            this.mis = mis;
            this.url = url;
        }

        public void onThrowable( Throwable t )
        {
            super.onThrowable( t );

            if ( mis != null )
            {
                mis.setException( t );
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

        public STATE onStatusReceived( HttpResponseStatus responseStatus )
            throws Exception
        {
            return handleStatus( url.toString(), responseStatus, mis );
        }

        public STATE onHeadersReceived( HttpResponseHeaders headers )
            throws Exception
        {
            STATE retval = super.onHeadersReceived( headers );
            FluentCaseInsensitiveStringsMap h = headers.getHeaders();

            if ( h.containsKey( "Content-Length" ) )
            {
                if ( mis != null )
                {
                    mis.setLength( Integer.parseInt( h.getFirstValue( "Content-Length" ) ) );
                }
            }
            
            if (this.encoding != null) {
            	his.encoding = encoding;
            }

            return retval;
        }

        public STATE onBodyPartReceived( HttpResponseBodyPart bodyPart )
            throws Exception
        {
            STATE retval = super.onBodyPartReceived( bodyPart );
            if ( os != null )
            {
                bodyPart.writeTo( os );
            }
            return retval;
        }

        public String onCompleted()
            throws Exception
        {
            close();
            return "";
        }
    }
}
