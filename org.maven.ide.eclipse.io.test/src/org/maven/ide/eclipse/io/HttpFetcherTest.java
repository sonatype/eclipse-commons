package org.maven.ide.eclipse.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.security.B64Code;
import org.eclipse.jetty.util.StringUtil;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.tests.common.HttpServer;

public class HttpFetcherTest
    extends TestCase
{
    private static final String AUTH_TYPE = "Basic ";

    HttpServer server;

    HttpFetcher fetcher;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        fetcher = new HttpFetcher();
        server = new HttpServer();
        server.enableRecording( new String[] { ".*" } );
        server.addUser( "validuser", "password", "validuser" );
        server.addSecuredRealm( "/secured/*", "validuser" );
        server.addResources( "/", "resources" );
        server.start();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
        }
        super.tearDown();
    }

    /*
     * Tests the error thrown when a file is not found.
     */
    public void testOpenstreamFileNotFound()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/nonExistentFile" );
        try
        {
            readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        }
        catch ( NotFoundException e )
        {
            assertTrue( e.getMessage().contains( String.valueOf( HttpStatus.NOT_FOUND_404 ) ) );
            assertTrue( e.getMessage().contains( HttpStatus.getMessage( HttpStatus.NOT_FOUND_404 ) ) );
            return;
        }
        fail( "NotFoundException should be thrown." );
    }

    /*
     * Tests the error thrown when a user does not have perission to access a file.
     */
    public void testOpenstreamForbidden()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/secured/secure.txt" );
        try
        {
            readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        }
        catch ( UnauthorizedException e )
        {
            assertTrue( e.getMessage().contains( String.valueOf( HttpStatus.UNAUTHORIZED_401 ) ) );
            assertTrue( e.getMessage().contains( HttpStatus.getMessage( HttpStatus.UNAUTHORIZED_401 ) ) );
            return;
        }
        fail( "UnauthorizedException should be thrown." );

    }

    /*
     * Tests the contents of a file remote file
     */
    public void testOpenStream()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/file.txt" );
        assertEquals( readstream( new FileInputStream( "resources/file.txt" ) ),
                     readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(),
                                                     null ) ) );
    }

    /*
     * Tests that the authentication header contains both a username and password.
     */
    public void testUsernameAndPasswordSent()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/file.txt" );
        setupAuth( address.toString(), "username", "password" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "username", "password", server.getRecordedHeaders( "/file.txt" ) );
    }

    /*
     * Tests that the authentication header only contains a username.
     */
    public void testUsernameOnly()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/file.txt" );
        setupAuth( address.toString(), "username", "" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "username", "", server.getRecordedHeaders( "/file.txt" ) );
    }

    /*
     * Tests that the authentication header only contains a password.
     */
    public void testPasswordOnly()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/file.txt" );
        setupAuth( address.toString(), "", "password" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "", "password", server.getRecordedHeaders( "/file.txt" ) );
    }

    /*
     * Tests that no header is set for anonymous authentication.
     */
    public void testAnonymous()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/file.txt" );
        setupAuth( address.toString(), "", "" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertNull( "No Auth header should be set",
                    server.getRecordedHeaders( "/file.txt" ).get( HttpHeaders.AUTHORIZATION ) );
    }

    /*
     * Assert that the authentication header is as expected
     */
    private static void assertAuthentication( String username, String password, Map<String, String> headers )
        throws UnsupportedEncodingException
    {
        String authHeader = headers.get( HttpHeaders.AUTHORIZATION );
        assertNotNull( "Authentication header was null", authHeader );
        assertTrue( "Authentication should be type: " + AUTH_TYPE, authHeader.startsWith( AUTH_TYPE ) );

        String decoded = B64Code.decode( authHeader.substring( AUTH_TYPE.length() ), StringUtil.__ISO_8859_1 );
        assertEquals( "Username does not match", username, decoded.substring( 0, decoded.indexOf( ':' ) ) );
        assertEquals( "Password does not match", password, decoded.substring( decoded.indexOf( ':' ) + 1 ) );
    }

    /*
     * Read a stream
     */
    private static String readstream( InputStream stream )
        throws IOException
    {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[128];
        int size = -1;
        while ( ( size = stream.read( buffer ) ) == 128 )
        {
            builder.append( new String( buffer, 0, size ) );
        }
        stream.close();
        if ( size != -1 )
            builder.append( new String( buffer, 0, size ) );
        return builder.toString();
    }

    /*
     * Set a username and password for a URL
     */
    private static void setupAuth( String url, String username, String password )
        throws Exception
    {
        IProgressMonitor monitor = new NullProgressMonitor();

        IAuthData authData = null;

        String realmId = url;
        IAuthRealm realm = AuthFacade.getAuthRegistry().getRealm( realmId );
        if ( realm == null )
        {
            realm =
                AuthFacade.getAuthRegistry().addRealm( realmId, realmId, realmId, AuthenticationType.USERNAME_PASSWORD,
                                                       monitor );
            AuthFacade.getAuthRegistry().addURLToRealmAssoc( url, realmId, AnonymousAccessType.NOT_ALLOWED, monitor );
        }
        else
        {
            authData = AuthFacade.getAuthService().select( url );
        }

        if ( authData == null )
        {
            authData = new AuthData( username, password, AnonymousAccessType.NOT_ALLOWED );
        }
        else
        {
            authData.setUsernameAndPassword( username, password );
        }
        AuthFacade.getAuthService().save( url, authData );
    }
}
