package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.security.B64Code;
import org.eclipse.jetty.util.StringUtil;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.tests.common.HttpServer;

public abstract class AbstractIOTest
    extends TestCase
{
    protected static final IProgressMonitor monitor = new NullProgressMonitor();

    /*
     * Password for secure remote server paths
     */
    protected static final String PASSWORD = "password";

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
            tmp.delete();
        tmp = new File( RESOURCES, SECURED_NEW_FILE.substring( 1 ) );
        if ( tmp.exists() )
            tmp.delete();
    }

    protected void startHttpServer()
        throws Exception
    {
        server = new HttpServer();
        server.addUser( VALID_USERNAME, PASSWORD, VALID_USERNAME );
        server.addSecuredRealm( "/secured/*", VALID_USERNAME );
        server.addResources( "/", "resources" );
        server.enableRecording( "/.*" );
        server.start();
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
        String authHeader = headers.get( HttpHeaders.AUTHORIZATION );
        assertNotNull( "Authentication header was null", authHeader );
        assertTrue( "Authentication should be type: " + AUTH_TYPE, authHeader.startsWith( AUTH_TYPE ) );

        String decoded = B64Code.decode( authHeader.substring( AUTH_TYPE.length() ), StringUtil.__ISO_8859_1 );
        assertEquals( "Username does not match", username, decoded.substring( 0, decoded.indexOf( ':' ) ) );
        assertEquals( "Password does not match", password, decoded.substring( decoded.indexOf( ':' ) + 1 ) );
    }
}
