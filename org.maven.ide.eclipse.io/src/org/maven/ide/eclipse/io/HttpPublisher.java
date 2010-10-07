package org.maven.ide.eclipse.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.maven.ide.eclipse.authentication.IAuthService;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;


public class HttpPublisher
    extends HttpBaseSupport
{
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
        return doDataExchange( file, url, monitor, monitorSubtaskName, authService, proxyService,
                               timeoutInMilliseconds, true, "PUT" );
    }

    public ServerResponse delete( final URI url, final IProgressMonitor monitor, String monitorSubtaskName,
                                  final IAuthService authService, final IProxyService proxyService,
                                  Integer timeoutInMilliseconds )
        throws IOException
    {
        return doDataExchange( null /* file */, url, monitor, monitorSubtaskName, authService, proxyService,
                               timeoutInMilliseconds, true, "DELETE" );
    }

    private ServerResponse doDataExchange( final RequestEntity file, final URI uri,
                                           final IProgressMonitor monitor,
                                   String monitorSubtaskName, final IAuthService authService,
                                   final IProxyService proxyService, Integer timeoutInMilliseconds, boolean statusException, String httpMethod )
        throws IOException
    {
    	AsyncHttpClientConfig.Builder confBuilder = init(uri, authService, proxyService, timeoutInMilliseconds);
    	AsyncHttpClientConfig conf = confBuilder.build();
    	
    	AsyncHttpClient httpClient = new AsyncHttpClient(conf);
    	FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();

		BoundRequestBuilder requestBuilder = null;
		
		String url = uri.toString();
		if ("PUT".equals(httpMethod)) {
			requestBuilder = httpClient.preparePut(url);
		} else if ("POST".equals(httpMethod)) {
			requestBuilder = httpClient.preparePost(url);
		} else if ("DELETE".equals(httpMethod)) {
			requestBuilder = httpClient.prepareDelete(url);
		} else if ("HEAD".equals(httpMethod)) {
			requestBuilder = httpClient.prepareHead(url);
		} else {
			throw new RuntimeException("Support for http method '" + httpMethod + "' not implemented.");
		}
				
		requestBuilder.setRealm(realm).setProxyServer(proxyServer);

		PushAsyncHandler handler = null;
		
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

            headers.add("Content-Length", Long.toString( file.getContentLength() ) );
            if ( file.getContentType() != null )
            {
            	headers.add("Content-Type", file.getContentType() );
            }

            requestBuilder.setBody(mis);
            handler = new PushAsyncHandler(uri, httpMethod, mis);
        } else {
        	handler = new PushAsyncHandler(uri, httpMethod, null);
        }

        //What's this for? (from previous Jetty code)
        //httpClient.registerListener( "org.eclipse.jetty.client.webdav.WebdavListener" );
		
		
		Future<HttpInputStream> future = requestBuilder.execute(handler);
		try {
			HttpInputStream his = future.get();
		} catch (InterruptedException e) {
            throw new IOException( "Transfer was interrupted" );
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		Throwable exception = handler.getException();
		if ( exception != null )
		{
			throw (IOException) new IOException( exception.getMessage() ).initCause( exception );
		}
		
		ServerResponse response =
			new ServerResponse( handler.getResponseStatus(), handler.getResponseContentBytes(),
					handler.getEncoding() );
		
        if ( statusException )
        {
            int status = handler.getResponseStatus();
            switch ( status )
            {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                case HttpURLConnection.HTTP_ACCEPTED:
                case HttpURLConnection.HTTP_NO_CONTENT:
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    throw new UnauthorizedException( "HTTP status code " + status + ": Unauthorized: " + uri );
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new ForbiddenException( "HTTP status code " + status + ": Forbidden: " + uri );
                case HttpURLConnection.HTTP_NOT_FOUND:
                    throw new NotFoundException( "HTTP status code " + status + ": Not Found: " + uri );
                default:
                    throw new TransferException( "HTTP status code " + status + ": " + uri, response, null );
            }
        }

        return response;
    }
    
    private final class PushAsyncHandler extends BaseAsyncHandler {
    	private final MonitoredInputStream mis;
		private URI uri;
		private String httpMethod;

		private Throwable exception; 
        private ByteArrayOutputStream baos = new ByteArrayOutputStream( 1024 );
		private int responseStatus;
    	
    	private PushAsyncHandler(URI uri, String httpMethod, MonitoredInputStream mis) {
    		this.mis = mis;
    		this.uri = uri;
    		this.httpMethod = httpMethod;
    	}

		public byte[] getResponseContentBytes() {
			return baos.toByteArray();
		}

		public int getResponseStatus() {
			return responseStatus;
		}

		public Throwable getException() {
			return exception;
		}

		@Override
		public void onThrowable(Throwable t) {
			super.onThrowable(t);
			error(t);
		}

		@Override
		public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart)
				throws Exception {
			STATE retval = super.onBodyPartReceived(bodyPart);
			bodyPart.writeTo(baos);
			return retval;
		}

		@Override
		public STATE onStatusReceived(HttpResponseStatus responseStatus)
				throws Exception {
			this.responseStatus = responseStatus.getStatusCode();
			return super.onStatusReceived(responseStatus);
		}

		@Override
		public STATE onHeadersReceived(HttpResponseHeaders headers)
				throws Exception {
			return super.onHeadersReceived(headers);
		}

		@Override
		public HttpInputStream onCompleted() throws Exception {
			return super.onCompleted();
		}

        private void error( Throwable e )
        {
            exception = e;
        }
    }


        /*public int waitForDone( IProgressMonitor monitor, String monitorTaskName )
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
        }*/

    public ServerResponse postFile( final RequestEntity file, final URI url, final IProgressMonitor monitor,
                                    String monitorSubtaskName, final IAuthService authService,
                                    final IProxyService proxyService, Integer timeoutInMilliseconds )
        throws IOException
    {
        return doDataExchange( file, url, monitor, monitorSubtaskName, authService, proxyService,
                               timeoutInMilliseconds, true, "POST" );
    }

    public ServerResponse headFile( final URI url, final IProgressMonitor monitor,
                                    String monitorSubtaskName, final IAuthService authService,
                                    final IProxyService proxyService, Integer timeoutInMilliseconds )
        throws IOException
    {
        return doDataExchange( null, url, monitor, monitorSubtaskName, authService, proxyService,
                               timeoutInMilliseconds, false, "HEAD" );
    }
}
