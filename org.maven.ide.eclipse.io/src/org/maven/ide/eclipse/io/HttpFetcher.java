package org.maven.ide.eclipse.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    public HttpInputStream openStream( final URI url, final IProgressMonitor monitor, final IAuthService authService,
                                       final IProxyService proxyService )
        throws IOException
    {
    	
        
        boolean followRedirects = true;
        int redirects = 3;

		AsyncHttpClientConfig.Builder confBuilder = init(url, authService, proxyService, null);

		AsyncHttpClientConfig conf = confBuilder
				.setFollowRedirects(followRedirects)
				.setMaximumNumberOfRedirects(redirects).build();

		AsyncHttpClient httpClient = new AsyncHttpClient(conf);

        PipedOutputStream os = new PipedOutputStream();
        final MonitoredInputStream mis = new MonitoredInputStream( new PipedInputStream( os ), monitor );

        FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
        headers.add( "Pragma", "no-cache" );
        headers.add( "Cache-Control", "no-cache, no-store" );

		BoundRequestBuilder requestBuilder = httpClient
				.prepareGet(url.toString())
				.setRealm(realm)
				.setHeaders(headers)
				.setProxyServer(proxyServer);
        
        Future<HttpInputStream> future = requestBuilder.execute(new GetAsyncHandler(os, mis, url));
        try {
			return future.get();
		} catch (ExecutionException e) {
			throw new IOException(e);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
    }

    private void handleStatus( String url, HttpResponseStatus responseStatus, MonitoredInputStream mis )
    {
    	int status = responseStatus.getStatusCode();
        if ( status != HttpURLConnection.HTTP_OK && mis != null )
        {
            if ( HttpURLConnection.HTTP_UNAUTHORIZED == status )
            {
                mis.setException( new UnauthorizedException( "HTTP status code " + status + ": "
                    + responseStatus.getStatusText() + ": " + url ) );
            }
            else if ( HttpURLConnection.HTTP_FORBIDDEN == status )
            {
                mis.setException( new ForbiddenException( "HTTP status code " + status + ": "
                    + responseStatus.getStatusText() + ": " + url ) );
            }
            else if ( HttpURLConnection.HTTP_NOT_FOUND == status )
            {
                mis.setException( new NotFoundException( "HTTP status code " + status + ": "
                    + responseStatus.getStatusText() + ": " + url ) );
            }
            else
            {
                mis.setException( new IOException( "HTTP status code " + status + ": " + responseStatus.getStatusText()
                    + ": " + url ) );
            }
        }
    }

    private final class GetAsyncHandler extends BaseAsyncHandler {
		private final MonitoredInputStream mis;
		private final OutputStream os;
		private final URI url;

		private GetAsyncHandler(OutputStream os, MonitoredInputStream mis, URI url) {
			this.os = os;
			this.mis = mis;
			this.url = url;
		}

		public void onThrowable(Throwable t) {
			super.onThrowable(t);
			
            if ( mis != null )
            {
                mis.setException( t );
            }
			
            close();
		}

		private void close() {
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

		public STATE onStatusReceived(HttpResponseStatus responseStatus)
				throws Exception {
			STATE retval = super.onStatusReceived(responseStatus);
			handleStatus(url.toString(), responseStatus, mis);
			return retval;
		}

		public STATE onHeadersReceived(HttpResponseHeaders headers)
				throws Exception {
			STATE retval = super.onHeadersReceived(headers);
			FluentCaseInsensitiveStringsMap h = headers.getHeaders();
			
			if (h.containsKey("Content-Length")) {
		        if ( mis != null )
		        {
		            mis.setLength( Integer.parseInt(h.getFirstValue("Content-Length")) );
		        }
			}
			
			return retval;
		}

		public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart)
		throws Exception {
			STATE retval = super.onBodyPartReceived(bodyPart);
			if ( os != null )
            {
            	bodyPart.writeTo( os );
            }
			return retval;
		}

		public HttpInputStream onCompleted() throws Exception {
			close();
			return new HttpInputStream( mis, encoding);
		}
	}
}
