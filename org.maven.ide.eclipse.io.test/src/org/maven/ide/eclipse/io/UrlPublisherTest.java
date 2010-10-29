package org.maven.ide.eclipse.io;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;

public class UrlPublisherTest
    extends AbstractIOTest
{
    private UrlPublisher publisher;

    public void setUp()
        throws Exception
    {
        super.setUp();
        publisher = new UrlPublisher();
    }

    public void testHttpPut()
        throws Exception
    {
        startHttpServer();
        URI url = URI.create( server.getHttpUrl() + FILE_PATH );
        publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, new NullProgressMonitor(),
                           AuthFacade.getAuthService(), null );
    }

    public void testHttpPutUnauthorized()
        throws Exception
    {
        startHttpServer();
        URI url = URI.create( server.getHttpUrl() + SECURE_FILE );
        try
        {
            publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url,
                               new NullProgressMonitor(), AuthFacade.getAuthService(), null );
            fail( "UnauthorizedException should be thrown." );
        }
        catch ( UnauthorizedException e )
        {
            e.printStackTrace();
            assertTrue( e.getMessage(), e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_UNAUTHORIZED ) ) );
        }
    }

    public void testHttpPutForbidden()
        throws Exception
    {
        server = newHttpServer();
        server.addResourceErrorResponse( FILE_PATH, HttpURLConnection.HTTP_FORBIDDEN );
        server.start();
        URI url = URI.create( server.getHttpUrl() + FILE_PATH );
        try
        {
            publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url,
                               new NullProgressMonitor(), AuthFacade.getAuthService(), null );
            fail( "ForbiddenException should be thrown." );
        }
        catch ( ForbiddenException e )
        {
            e.printStackTrace();
            assertTrue( e.getMessage(), e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_FORBIDDEN ) ) );
        }
    }

    public void testHttpPut400()
        throws Exception
    {
        server = newHttpServer();
        server.addResourceErrorResponse( FILE_PATH, HttpURLConnection.HTTP_BAD_REQUEST );
        server.start();
        URI url = URI.create( server.getHttpUrl() + FILE_PATH );
        try
        {
            publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url,
                               new NullProgressMonitor(), AuthFacade.getAuthService(), null );
            fail( "TransferException should be thrown." );
        }
        catch ( TransferException e )
        {
            e.printStackTrace();
            assertTrue( e.getMessage(), e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_BAD_REQUEST ) ) );
            ServerResponse response = e.getServerResponse();
            assertNotNull( "Expected not null response", response );
            byte[] responseData = response.getResponseData();
            assertNotNull( "Expected not null response data", responseData );
            assertEquals( "***Error400***", new String( responseData ) );
        }
    }

    /*
     * Tests that the authentication header contains both a username and password.
     */
    public void testHttpPutUsernameAndPasswordSent()
        throws Exception
    {
        startHttpServer();
        URI url = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testHttpPutUsernameAndPasswordSent", url.toString(), "username", "password" );
        publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, new NullProgressMonitor(),
                           AuthFacade.getAuthService(), null );
        assertAuthentication( "username", "password", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that the authentication header only contains a username.
     */
    public void testHttpPutUsernameOnly()
        throws Exception
    {
        startHttpServer();
        URI url = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testHttpPutUsernameOnly", url.toString(), "username", "" );
        publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, new NullProgressMonitor(),
                           AuthFacade.getAuthService(), null );
        assertAuthentication( "username", "", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that the authentication header only contains a password.
     */
    public void testHttpPutPasswordOnly()
        throws Exception
    {
        startHttpServer();
        URI url = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testHttpPutUsernameAndPasswordSent", url.toString(), "", "password" );
        publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, new NullProgressMonitor(),
                           AuthFacade.getAuthService(), null );
        assertAuthentication( "", "password", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that no header is set for anonymous authentication.
     */
    public void testHttpPutAnonymous()
        throws Exception
    {
        startHttpServer();
        URI url = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testHttpPutUsernameAndPasswordSent", url.toString(), "", "" );
        publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, new NullProgressMonitor(),
                           AuthFacade.getAuthService(), null );
        assertNull( "No Auth header should be set", server.getRecordedHeaders( FILE_PATH ).get( "Authorization" ) );
    }
}
