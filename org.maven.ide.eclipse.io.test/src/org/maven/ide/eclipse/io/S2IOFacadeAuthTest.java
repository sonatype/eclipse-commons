package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSuiteConfigurator;

// resource loading does not work properly for plugin tests
// @ConfiguratorList( "resources/S2IOFacadeAuthTestConfigurators.list" )
@Configurators( { BasicAuthSuiteConfigurator.class, DigestAuthSuiteConfigurator.class,
    BasicAuthSslSuiteConfigurator.class, DigestAuthSslSuiteConfigurator.class } )
public class S2IOFacadeAuthTest
    extends AbstractIOTest
{

    private static String VALID_USERNAME = "user";

    private static String PASSWORD = "password";

    public void testDeleteRequest_ValidUser()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + SECURE_FILE );
        addRealmAndURL( "testDeleteRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.delete( address.toString(), null, monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_OK, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "DELETE", address.toString() );
    }

    public void testDeleteRequest_ValidUser_NotFound()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/secured/missingFile.txt" );
        addRealmAndURL( "testDeleteRequest_ValidUser_NotFound", address.toString(), VALID_USERNAME, PASSWORD );
        try
        {
            S2IOFacade.delete( address.toString(), null, monitor, "Monitor name" );
            fail( "NotFoundException should have been thrown" );
        }
        catch ( NotFoundException e )
        {
            assertRequest( "Unexpected recorded request", "DELETE", address.toString() );
        }
    }

    public void testExists_ValidUser()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testExists_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        assertTrue( S2IOFacade.exists( address.toString(), monitor ) );
    }

    public void testExists_ValidUser_NotFound()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + NEW_FILE );
        addRealmAndURL( "testExists_ValidUser_NotFound", address.toString(), VALID_USERNAME, PASSWORD );
        assertFalse( S2IOFacade.exists( address.toString(), monitor ) );
    }

    /*
     * There seems to be an bug with the Jetty server & a head request with an invalid username/password combo on a
     * protected resource.
     */
    public void testHeadRequest_InvalidUser()
        throws Exception
    {
        String url = server.getHttpUrl() + SECURE_FILE;
        addRealmAndURL( "testHeadRequest_InvalidUser", url, "invalidusername", "invalidpassword" );
        ServerResponse resp = S2IOFacade.head( url, null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_UNAUTHORIZED, resp.getStatusCode() );
    }

    public void testHeadRequest_ValidUser()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + SECURE_FILE );
        addRealmAndURL( "testHeadRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_OK, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "HEAD", address.toString() );
    }

    public void testHeadRequest_ValidUser_NotFound()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/secured/missingFile.txt" );
        addRealmAndURL( "testHeadRequest_ValidUser_NotFound", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_NOT_FOUND, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "HEAD", address.toString() );
    }

    public void testOpenStream_InvalidUser()
        throws Exception
    {
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
                        e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_UNAUTHORIZED ) ) );
        }
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

    public void testPostRequest_ValidUser()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + SECURED_NEW_FILE );
        addRealmAndURL( "testPostRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp =
            S2IOFacade.post( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), address.toString(), null,
                             monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_CREATED, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "POST", address.toString() );
    }

    public void testPutRequest_ValidUser()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + SECURED_NEW_FILE );
        addRealmAndURL( "testPutRequest_ValidUser", address.toString(), VALID_USERNAME, PASSWORD );
        ServerResponse resp =
            S2IOFacade.put( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), address.toString(), null,
                             monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_CREATED, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "PUT", address.toString() );
    }

    public void testPutRequest_InvalidUser()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + SECURED_NEW_FILE );
        addRealmAndURL( "testPutRequest_InvalidUser", address.toString(), "invalidusername", "invalidpassword" );
        try
        {
            ServerResponse response = S2IOFacade.put( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), address.toString(), null,
                                monitor, "Monitor name" );
            assertEquals( 401, response.getStatusCode() );
            fail( "An UnauthorizedException should have been thrown" );
        }
        catch ( UnauthorizedException e )
        {
            assertTrue( "Missing http status code",
                        e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_UNAUTHORIZED ) ) );
        }
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( S2IOFacadeAuthTest.class );
    }
}
