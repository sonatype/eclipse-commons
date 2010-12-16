package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

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
import org.osgi.util.tracker.ServiceTracker;
import org.sonatype.tests.http.runner.AbstractSuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.server.api.ServerProvider;
import org.sonatype.tests.http.server.jetty.behaviour.Record;
import org.sonatype.tests.http.server.jetty.behaviour.filesystem.Delete;
import org.sonatype.tests.http.server.jetty.behaviour.filesystem.Get;
import org.sonatype.tests.http.server.jetty.behaviour.filesystem.Head;
import org.sonatype.tests.http.server.jetty.behaviour.filesystem.Post;
import org.sonatype.tests.http.server.jetty.behaviour.filesystem.Put;

import com.ning.http.util.Base64;

/**
 * Basic test class.
 * <p>
 * The purpose of the http-testing-harness is executing the exact same test cases in different server environments.
 * </p>
 * <p>
 * Instead of having a method to set up a server instance that is called before every test method, the server set up is
 * handled by {@link Configurators} annotated at the test class. The super class (*SuiteConfiguration) handles start,
 * stop and configuration of the behavior of server instances.
 * </p>
 * <p>
 * The server instances can be configured by providing behaviors that may be chained for a pathspec. They are called
 * from within a Servlet and may act accordingly. Overwrite
 * {@link Junit3SuiteConfiguration#configureProvider(ServerProvider)} for a central configuration, although single test
 * methods may add behavior as well.
 * </p>
 * <p>
 * For JUnit3, every class has to inherit from 'Junit3ServerConfiguration' and provide a static suite-method that
 * delegates to {@link Junit3SuiteConfiguration#suite(Class)}.
 * </p>
 * 
 * @see AbstractSuiteConfiguration
 * @see Configurators
 * @see Junit3SuiteConfiguration#suite(Class)
 * @see ServerProvider#addBehaviour(String, org.sonatype.tests.http.server.api.Behaviour...)
 * @see ServerProvider#addAuthentication(String, String)
 * @see ServerProvider#addUser(String, Object)
 */
public abstract class AbstractIOTest
    extends Junit3SuiteConfiguration
{
    protected class ServerProviderWrapper
    {

        private final ServerProvider provider;

        public ServerProviderWrapper( ServerProvider provider )
        {
            this.provider = provider;
        }

        public URL getHttpUrl()
        {
            try
            {
                return provider.getUrl();
            }
            catch ( MalformedURLException e )
            {
                throw new IllegalStateException( "ServerProvider was not correctly initialized: " + e.getMessage(), e );
            }
        }

        public Map<String, String> getRecordedHeaders( String filePath )
        {
            return recorder.getRequestHeaders().get( filePath );
        }

        public List<String> getRecordedRequests()
        {
            return recorder.getRequests();
        }
    }

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

    protected ServerProviderWrapper server;

    protected Record recorder;

    protected ServiceTracker proxyServiceTracker;

    @Override
    public void setUp()
        throws Exception
    {

        super.setUp();

        proxyServiceTracker =
            new ServiceTracker( S2IOPlugin.getDefault().getBundle().getBundleContext(), IProxyService.class.getName(),
                                null );
        proxyServiceTracker.open();

        resetProxies();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        AuthFacade.getAuthRegistry().clear();

        if ( provider() != null )
        {
            provider().stop();
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
            {
                builder.append( new String( buffer, 0, size ) );
            }
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

        String decoded = new String( Base64.decode( authHeader.substring( AUTH_TYPE.length() ) ) );
        assertEquals( "Username does not match", username, decoded.substring( 0, decoded.indexOf( ':' ) ) );
        assertEquals( "Password does not match", password, decoded.substring( decoded.indexOf( ':' ) + 1 ) );
    }

    @Override
    public void configureProvider( ServerProvider provider )
    {
        super.configureProvider( provider );
        recorder = new Record();
        String fsPath = "resources";
        provider().addBehaviour( "/*", recorder, new Get( fsPath ), new Post( fsPath ), new Put( fsPath ),
                                 new Head( fsPath ), new Delete( fsPath ) );
        server = new ServerProviderWrapper( provider() );
    }

    protected void setProxy( String host, int port, boolean ssl, String username, String password )
        throws Exception
    {
        IProxyService proxyService = getProxyService();
        assertNotNull( proxyService );

        @SuppressWarnings( "restriction" )
        IProxyData data =
            new ProxyData( ssl ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE, host, port,
                           ( username != null ), null );
        data.setUserid( username );
        data.setPassword( password );
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

            @SuppressWarnings( "restriction" )
            IProxyData[] data =
                new IProxyData[] { new ProxyData( IProxyData.HTTP_PROXY_TYPE ),
                    new ProxyData( IProxyData.HTTPS_PROXY_TYPE ) };

            proxyService.setProxyData( data );
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
