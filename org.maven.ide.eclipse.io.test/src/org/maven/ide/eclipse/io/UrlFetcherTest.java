package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.maven.ide.eclipse.authentication.AuthFacade;

public class UrlFetcherTest
    extends AbstractIOTest
{
    private UrlFetcher fetcher;

    public void setUp()
        throws Exception
    {
        super.setUp();
        fetcher = new UrlFetcher();
    }

    /*
     * Tests the error thrown when a file is not found.
     */
    @Test
    public void testHttpOpenstreamFileNotFound()
        throws Exception
    {
        startHttpServer();
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
    public void testHttpOpenstreamForbidden()
        throws Exception
    {
        startHttpServer();
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
    public void testHttpOpenStream()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        assertEquals( readstream( new FileInputStream( "resources/file.txt" ) ),
                     readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(),
                                                     null ) ) );
    }

    /*
     * Tests that the authentication header contains both a username and password.
     */
    public void testHttpUsernameAndPasswordSent()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testUsernameAndPasswordSent", address.toString(), "username", "password" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "username", "password", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that the authentication header only contains a username.
     */
    public void testHttpUsernameOnly()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testUsernameOnly", address.toString(), "username", "" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "username", "", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that the authentication header only contains a password.
     */
    public void testHttpPasswordOnly()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testPasswordOnly", address.toString(), "", "password" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertAuthentication( "", "password", server.getRecordedHeaders( FILE_PATH ) );
    }

    /*
     * Tests that no header is set for anonymous authentication.
     */
    public void testHttpAnonymous()
        throws Exception
    {
        startHttpServer();
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testAnonymous", address.toString(), "", "" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertNull( "No Auth header should be set",
                    server.getRecordedHeaders( FILE_PATH ).get( HttpHeaders.AUTHORIZATION ) );
    }

    /*
     * Tests reading a stream from a local file.
     */
    public void testFileOpenStream()
        throws Exception
    {
        assertEquals( readstream( new FileInputStream( "resources/file.txt" ) ),
                      readstream( fetcher.openStream( new File( RESOURCES, "file.txt" ).toURI(), monitor,
                                                      AuthFacade.getAuthService(), null ) ) );
    }
}
