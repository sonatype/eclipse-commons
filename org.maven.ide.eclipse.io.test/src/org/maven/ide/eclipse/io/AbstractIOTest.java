package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.io.internal.S2IOPlugin;
import org.maven.ide.eclipse.tests.common.HttpServer;
import org.osgi.util.tracker.ServiceTracker;

import com.ning.http.util.Base64;

public abstract class AbstractIOTest
    extends TestCase
{
    protected static final IProgressMonitor monitor = new NullProgressMonitor();

    /*
     * Password for secure remote server paths
     */
    protected static final String PASSWORD = "password";
    
    protected static final String KEY_PASSWORD = "keypwd";
    
    protected static final String STORE_PASSWORD = "storepwd";

    /*
     * User for secure remote server paths
     */
    protected static final String VALID_USERNAME = "validuser";

    /*
     * Path to anonymous file
     */
    protected static final String FILE_PATH = "/file.txt";

    /*
     * Local path to file.txt
     */
    protected static final String FILE_LOCAL = "file.txt";

    /*
     * Path to secured file
     */
    protected static final String SECURE_FILE = "/secured/secure.txt";

    /*
     * Path to upload unsecured files, deleted on tear-down.
     */
    protected static final String NEW_FILE = "/unusedFile.txt";

    /*
     * Path to upload secured files, deleted on tear-down.
     */
    protected static final String SECURED_NEW_FILE = "/secured/unusedFile.txt";

    /*
     * Local resources directory
     */
    protected static File RESOURCES = new File( "resources" ).getAbsoluteFile();

    private static final String AUTH_TYPE = "Basic ";

    protected HttpServer server;

	protected ServiceTracker proxyServiceTracker;
    
    @Override
    protected void setUp() throws Exception {
    	
    	System.out.println( "TEST-SETUP: " + getName() );

        super.setUp();
        
        proxyServiceTracker =
            new ServiceTracker( S2IOPlugin.getDefault().getBundle().getBundleContext(),
                                IProxyService.class.getName(), null );
        proxyServiceTracker.open();
        
        resetProxies();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        AuthFacade.getAuthRegistry().clear();

        if ( server != null )
        {
            server.stop();
        }
        File tmp = new File( RESOURCES, NEW_FILE.substring( 1 ) );
        if ( tmp.exists() )
        {
            tmp.delete();
        }
        tmp = new File( RESOURCES, SECURED_NEW_FILE.substring( 1 ) );
        if ( tmp.exists() )
        {
            tmp.delete();
        }
        
        resetProxies();

        if ( proxyServiceTracker != null )
        {
            proxyServiceTracker.close();
            proxyServiceTracker = null;
        }
    }
    
    protected void startHttpServer() throws Exception {
    	server = newHttpServer();
    	server.start();
    }

    protected HttpServer newHttpServer()
        throws Exception
    {
        server = new HttpServer();
        server.setHttpsPort(0);
        server.addUser( VALID_USERNAME, PASSWORD, VALID_USERNAME );
        server.addSecuredRealm( "/secured/*", VALID_USERNAME );
        server.addResources( "/", "resources" );
        server.enableRecording( "/.*" );
        server.setStorePassword(STORE_PASSWORD);
        server.setKeyStore("resources/ssl/keystore", KEY_PASSWORD);
        return server;
    }

    /**
     * Assert the last server request was using a method & url.
     * 
     * @param message the error message
     * @param httpMethod the http method used by the request, use {@code HttpMethod} constants
     * @param url the server url the request was made to
     */
    protected void assertRequest( String message, String httpMethod, String url )
    {
        List<String> requests = server.getRecordedRequests();
        assertTrue( "No requests recorded", !requests.isEmpty() );
        assertEquals( message, httpMethod + ' ' + URI.create( url ).getPath(), requests.get( 0 ) );
    }

    protected static String readstream( InputStream stream )
        throws IOException
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            byte[] buffer = new byte[128];
            int size = -1;
            while ( ( size = stream.read( buffer ) ) == 128 )
            {
                builder.append( new String( buffer, 0, size ) );
            }
            if ( size != -1 )
                builder.append( new String( buffer, 0, size ) );
            return builder.toString();
        }
        finally
        {
            stream.close();
        }
    }

    /*
     * Create realm for URL with username & password.
     */
    protected static void addRealmAndURL( String realmId, String url, String username, String password )
    {
        addRealmAndURL( realmId, url, AuthenticationType.USERNAME_PASSWORD, AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthService().save( url, username, password );
    }

    protected static void addRealmAndURL( String realmId, String url, AuthenticationType type,
                                          AnonymousAccessType anonType )
    {
        IAuthRealm realm = AuthFacade.getAuthRegistry().addRealm( realmId, realmId, realmId, type, monitor );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( url, realm.getId(), anonType, monitor );
    }

    /*
     * Assert that the authentication header is as expected
     */
    protected static void assertAuthentication( String username, String password, Map<String, String> headers )
        throws UnsupportedEncodingException
    {
        String authHeader = headers.get( "Authorization" );
        assertNotNull( "Authentication header was null", authHeader );
        assertTrue( "Authentication should be type: " + AUTH_TYPE, authHeader.startsWith( AUTH_TYPE ) );

        String decoded = new String(Base64.decode( authHeader.substring( AUTH_TYPE.length() )));
        assertEquals( "Username does not match", username, decoded.substring( 0, decoded.indexOf( ':' ) ) );
        assertEquals( "Password does not match", password, decoded.substring( decoded.indexOf( ':' ) + 1 ) );
    }
    
    protected void setProxy( String host, int port, boolean ssl, String username, String password )
    throws Exception
	{
	    IProxyService proxyService = getProxyService();
	    assertNotNull(proxyService);
	    
        @SuppressWarnings("restriction")
		IProxyData data =
            new ProxyData( ssl ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE, host, port, (username != null), null);
        data.setUserid(username);
        data.setPassword(password);
        proxyService.setProxyData( new IProxyData[] { data } );
    
	}
    
    protected void resetProxies()
    throws Exception
	{
	    IProxyService proxyService = getProxyService();
	    if ( proxyService != null )
	    {
	        proxyService.setSystemProxiesEnabled( false );
	        proxyService.setNonProxiedHosts( new String[0] );
	        proxyService.setProxyData( new IProxyData[] { new ProxyData( IProxyData.HTTP_PROXY_TYPE ),
	            new ProxyData( IProxyData.HTTPS_PROXY_TYPE ) } );
	    }
	}

    
    protected IProxyService getProxyService()
    {
        return ( proxyServiceTracker != null ) ? (IProxyService) proxyServiceTracker.getService() : null;
    }
    
    protected void setNonProxiedHosts( String... hosts )
    throws Exception
	{
	    IProxyService proxyService = getProxyService();
	    if ( proxyService != null )
	    {
	        proxyService.setNonProxiedHosts( hosts );
	    }
	}
}
