package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;

public class S2IOFacadeTest
    extends AbstractIOTest
{
    public void testHeadRequest_Anonymous()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testHeadRequest_Anonymous", url, "", "" );
        ServerResponse resp = S2IOFacade.head( url, null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.OK_200, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.HEAD, url );
    }

    public void testHeadRequest_Local()
        throws Exception
    {
        URI address = new File( RESOURCES, FILE_LOCAL ).toURI();
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.OK_200, resp.getStatusCode() );
    }

    public void testDeleteRequest_Anonymous()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testDeleteRequest_Anonymous", url, "", "" );
        ServerResponse resp = S2IOFacade.delete( url, null, monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpStatus.OK_200, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.DELETE, url );
    }

    public void testPostRequest_Anonymous()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testPostRequest_Anonymous", url, "", "" );
        ServerResponse resp =
            S2IOFacade.post( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, null, monitor,
                             "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpStatus.CREATED_201, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.POST, url );
    }

    public void testPutRequest_Anonymous()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testPutRequest_Anonymous", url, "", "" );
        ServerResponse resp =
            S2IOFacade.put( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, null, monitor,
                            "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpStatus.CREATED_201, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.PUT, url );
    }

    public void testHeadRequest_Anonymous_NotFound()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testHeadRequest_Anonymous_NotFound", url, "", "" );
        ServerResponse resp = S2IOFacade.head( url, null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.NOT_FOUND_404, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.HEAD, url );
    }

    public void testHeadRequest_Local_NotFound()
        throws Exception
    {
        URI address = new File( RESOURCES, "missingfile.txt" ).toURI();
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.NOT_FOUND_404, resp.getStatusCode() );
    }

    public void testDeleteRequest_Anonymous_NotFound()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testDeleteRequest_Anonymous_NotFound", url, "", "" );
        try
        {
            S2IOFacade.delete( url, null, monitor, "Monitor name" );
            fail( "NotFoundException should have been thrown" );
        }
        catch ( NotFoundException e )
        {
            assertRequest( "Unexpected recorded request", HttpMethods.DELETE, url );
        }
    }

    public void testHeadRequest_ValidUser()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + SECURE_FILE );
        addRealmAndURL( "testHeadRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.OK_200, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.HEAD, address.toString() );
    }

    public void testDeleteRequest_ValidUser()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + SECURE_FILE );
        addRealmAndURL( "testDeleteRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.delete( address.toString(), null, monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpStatus.OK_200, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.DELETE, address.toString() );
    }

    public void testPostRequest_ValidUser()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + SECURED_NEW_FILE );
        addRealmAndURL( "testPostRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp =
            S2IOFacade.post( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), address.toString(), null,
                             monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpStatus.CREATED_201, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.POST, address.toString() );
    }

    public void testPutRequest_ValidUser()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + SECURED_NEW_FILE );
        addRealmAndURL( "testPutRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp =
            S2IOFacade.put( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), address.toString(), null,
                             monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpStatus.CREATED_201, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.PUT, address.toString() );
    }

    public void testHeadRequest_ValidUser_NotFound()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + "/secured/missingFile.txt" );
        addRealmAndURL( "testHeadRequest_ValidUser_NotFound", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpStatus.NOT_FOUND_404, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", HttpMethods.HEAD, address.toString() );
    }

    public void testDeleteRequest_ValidUser_NotFound()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + "/secured/missingFile.txt" );
        addRealmAndURL( "testDeleteRequest_ValidUser_NotFound", address.toString(), VALID_USERNAME, PASSWORD );
        try
        {
            S2IOFacade.delete( address.toString(), null, monitor, "Monitor name" );
            fail( "NotFoundException should have been thrown" );
        }
        catch ( NotFoundException e )
        {
            assertRequest( "Unexpected recorded request", HttpMethods.DELETE, address.toString() );
        }
    }

    public void testExists()
        throws Exception
    {
        startHttpServer();
        assertTrue( S2IOFacade.exists( URI.create( server.getHttpUrl() + FILE_PATH ).toString(), monitor ) );
    }

    public void testExists_ValidUser()
        throws Exception
    {
        startHttpServer();
        assertTrue( S2IOFacade.exists( URI.create( server.getHttpUrl() + FILE_PATH ).toString(), monitor ) );
    }

    public void testExists_Local()
        throws Exception
    {
        assertTrue( S2IOFacade.exists( new File( RESOURCES, FILE_LOCAL ).toURI().toString(), monitor ) );
    }

    public void testExists_NotFound()
        throws Exception
    {
        startHttpServer();
        assertFalse( S2IOFacade.exists( URI.create( server.getHttpUrl() + NEW_FILE ).toString(), monitor ) );
    }

    public void testExists_ValidUser_NotFound()
        throws Exception
    {
        startHttpServer();
        assertFalse( S2IOFacade.exists( URI.create( server.getHttpUrl() + NEW_FILE ).toString(), monitor ) );
    }

    public void testExists_Local_NotFound()
        throws Exception
    {
        assertFalse( S2IOFacade.exists( new File( RESOURCES, NEW_FILE ).toURI().toString(), monitor ) );
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
        startHttpServer();
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testOpenStream_Anonymous", url, "", "" );
        assertEquals( "Content of stream differs from file", readstream( new FileInputStream( "resources/file.txt" ) ),
                      readstream( S2IOFacade.openStream( url, monitor ) ) );
        assertRequest( "Unexpected recorded request", HttpMethods.GET, url );
    }

    public void testOpenStream_Anonymous_NotFound()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testOpenStream_Anonymous_NotFound", url, "", "" );
        try
        {
            readstream( S2IOFacade.openStream( url, monitor ) );
            fail( "A NotFoundException should have been thrown" );
        }
        catch ( NotFoundException expected )
        {
        }
    }

    public void testOpenStream_ValidUser()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + SECURE_FILE;
        addRealmAndURL( "testOpenStream_ValidUser", url, VALID_USERNAME, PASSWORD );
        assertEquals( "Content of stream differs from file",
                      readstream( new FileInputStream( "resources" + SECURE_FILE ) ),
                      readstream( S2IOFacade.openStream( url, monitor ) ) );
    }

    public void testOpenStream_ValidUser_NotFound()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testOpenStream_ValidUser_NotFound", url, VALID_USERNAME, PASSWORD );
        try
        {
            readstream( S2IOFacade.openStream( url, monitor ) );
            fail( "A NotFoundException should have been thrown" );
        }
        catch ( NotFoundException expected )
        {
        }
    }

    public void testOpenStream_InvalidUser()
        throws Exception
    {
        startHttpServer();
        String url = server.getHttpUrl() + SECURE_FILE;
        addRealmAndURL( "testOpenStream_InvalidUser", url, "invalidusername", "invalidpassword" );
        try
        {
            readstream( S2IOFacade.openStream( url, monitor ) );
            fail( "An UnauthorizedException should have been thrown" );
        }
        catch ( UnauthorizedException e )
        {
            assertTrue( "Missing http status code",
                        e.getMessage().contains( String.valueOf( HttpStatus.UNAUTHORIZED_401 ) ) );
        }
    }
}
