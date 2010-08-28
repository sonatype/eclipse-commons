package org.maven.ide.eclipse.io;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.security.B64Code;
import org.eclipse.jetty.util.StringUtil;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.tests.common.HttpServer;

public class HttpFetcherTest
    extends AbstractIOTest
{

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
        URI address = URI.create( server.getHttpUrl() + SECURE_FILE );
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
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
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
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testUsernameAndPasswordSent", address.toString(), "username", "password" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "username", "password", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that the authentication header only contains a username.
     */
    public void testUsernameOnly()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testUsernameOnly", address.toString(), "username", "" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "username", "", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that the authentication header only contains a password.
     */
    public void testPasswordOnly()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testPasswordOnly", address.toString(), "", "password" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "", "password", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that no header is set for anonymous authentication.
     */
    public void testAnonymous()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testAnonymous", address.toString(), "", "" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertNull( "No Auth header should be set",
                    server.getRecordedHeaders( FILE_PATH ).get( HttpHeaders.AUTHORIZATION ) );
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
}
