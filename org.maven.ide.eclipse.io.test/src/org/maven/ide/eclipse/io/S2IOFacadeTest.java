package org.maven.ide.eclipse.io;

import java.io.FileInputStream;
import java.net.URI;

import org.eclipse.jetty.http.HttpStatus;
import org.maven.ide.eclipse.tests.common.HttpServer;

public class S2IOFacadeTest
    extends AbstractIOTest
{
    private static final String PASSWORD = "password";
    private static final String VALID_USERNAME = "validuser";
    private HttpServer server;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        server = new HttpServer();
        server.addUser( VALID_USERNAME, PASSWORD, VALID_USERNAME );
        server.addSecuredRealm( "/secured/*", VALID_USERNAME );
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

    public void testHeadRequest_Anonymous()
        throws Exception
    {
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testHeadRequest_Anonymous", url, "", "" );
        ServerResponse resp = S2IOFacade.head( url, null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.OK_200, resp.getStatusCode() );
    }

    public void testHeadRequest_Anonymous_NotFound()
        throws Exception
    {
        String url = server.getHttpUrl() + "/missingFile.txt";
        addRealmAndURL( "testHeadRequest_Anonymous_NotFound", url, "", "" );
        ServerResponse resp = S2IOFacade.head( url, null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.NOT_FOUND_404, resp.getStatusCode() );
    }

    public void testHeadRequest_ValidUser()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + SECURE_FILE );
        addRealmAndURL( "testHeadRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.OK_200, resp.getStatusCode() );
    }

    public void testHeadRequest_ValidUser_NotFound()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/secured/missingFile.txt" );
        addRealmAndURL( "testHeadRequest_ValidUser_NotFound", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.NOT_FOUND_404, resp.getStatusCode() );
    }

    /*
     * There seems to be an bug with the Jetty server & a head request with an invalid username/password combo on a
     * protected resource.
     */
    // public void testHeadRequest_InvalidUser()
    // throws Exception
    // {
    // String url = server.getHttpUrl() + SECURE_FILE;
    // addRealmAndURL( "testHeadRequest_InvalidUser", url, "invalidusername", "invalidpassword" );
    // ServerResponse resp = S2IOFacade.head( url, null, monitor );
    // assertEquals( "Unexpected HTTP status code", HttpStatus.UNAUTHORIZED_401, resp.getStatusCode() );
    // }

    public void testOpenStream_Anonymous()
        throws Exception
    {
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testOpenStream_Anonymous", url, "", "" );
        assertEquals( "Content of stream differs from file", readstream( new FileInputStream( "resources/file.txt" ) ),
                      readstream( S2IOFacade.openStream( url, monitor ) ) );
    }

    public void testOpenStream_Anonymous_NotFound()
        throws Exception
    {
        String url = server.getHttpUrl() + "/missingFile.txt";
        addRealmAndURL( "testOpenStream_Anonymous_NotFound", url, "", "" );
        try
        {
            readstream( S2IOFacade.openStream( url, monitor ) );
        }
        catch ( NotFoundException e )
        {
            return;
        }
        fail( "A NotFoundException should have been thrown" );
    }

    public void testOpenStream_ValidUser()
        throws Exception
    {
        String url = server.getHttpUrl() + SECURE_FILE;
        addRealmAndURL( "testOpenStream_ValidUser", url, VALID_USERNAME, PASSWORD );
        assertEquals( "Content of stream differs from file",
                      readstream( new FileInputStream( "resources" + SECURE_FILE ) ),
                      readstream( S2IOFacade.openStream( url, monitor ) ) );
    }

    public void testOpenStream_ValidUser_NotFound()
        throws Exception
    {
        String url = server.getHttpUrl() + "/missingFile.txt";
        addRealmAndURL( "testOpenStream_ValidUser_NotFound", url, VALID_USERNAME, PASSWORD );
        try
        {
            readstream( S2IOFacade.openStream( url, monitor ) );
        }
        catch ( NotFoundException e )
        {
            return;
        }
        fail( "A NotFoundException should have been thrown" );

    }

    public void testOpenStream_InvalidUser()
        throws Exception
    {
        String url = server.getHttpUrl() + SECURE_FILE;
        addRealmAndURL( "testOpenStream_InvalidUser", url, "invalidusername", "invalidpassword" );
        try
        {
            readstream( S2IOFacade.openStream( url, monitor ) );
        }
        catch ( UnauthorizedException e )
        {
            assertTrue( "Missing http status code",
                        e.getMessage().contains( String.valueOf( HttpStatus.UNAUTHORIZED_401 ) ) );
            return;
        }
        fail( "An UnauthorizedException should have been thrown" );
    }
}
