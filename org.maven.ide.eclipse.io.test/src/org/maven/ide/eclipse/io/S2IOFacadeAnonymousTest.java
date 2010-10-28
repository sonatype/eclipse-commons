package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.DefaultSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.SslSuiteConfigurator;

@Configurators( { DefaultSuiteConfigurator.class, SslSuiteConfigurator.class } )
public class S2IOFacadeAnonymousTest
    extends AbstractIOTest
{

    public void testDeleteRequest_Anonymous()
        throws Exception
    {
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testDeleteRequest_Anonymous", url, "", "" );
        ServerResponse resp = S2IOFacade.delete( url, null, monitor, "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_OK, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "DELETE", url );
    }

    public void testDeleteRequest_Anonymous_NotFound()
        throws Exception
    {
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testDeleteRequest_Anonymous_NotFound", url, "", "" );
        try
        {
            S2IOFacade.delete( url, null, monitor, "Monitor name" );
            fail( "NotFoundException should have been thrown" );
        }
        catch ( NotFoundException e )
        {
            assertRequest( "Unexpected recorded request", "DELETE", url );
        }
    }

    public void testHeadRequest_Anonymous()
        throws Exception
    {
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testHeadRequest_Anonymous", url, "", "" );
        ServerResponse resp = S2IOFacade.head( url, null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_OK, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "HEAD", url );
    }

    public void testHeadRequest_Anonymous_NotFound()
        throws Exception
    {
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testHeadRequest_Anonymous_NotFound", url, "", "" );
        ServerResponse resp = S2IOFacade.head( url, null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_NOT_FOUND, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "HEAD", url );
    }

    public void testOpenStream_Anonymous()
        throws Exception
    {
        String url = server.getHttpUrl() + FILE_PATH;
        addRealmAndURL( "testOpenStream_Anonymous", url, "", "" );
        assertEquals( "Content of stream differs from file", readstream( new FileInputStream( "resources/file.txt" ) ),
                      readstream( S2IOFacade.openStream( url, monitor ) ) );
        assertRequest( "Unexpected recorded request", "GET", url );
    }

    public void testOpenStream_Anonymous_NotFound()
        throws Exception
    {
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

    public void testPostRequest_Anonymous()
        throws Exception
    {
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testPostRequest_Anonymous", url, "", "" );
        ServerResponse resp =
            S2IOFacade.post( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, null, monitor,
                             "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_CREATED, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "POST", url );
    }

    public void testPutRequest_Anonymous()
        throws Exception
    {
        String url = server.getHttpUrl() + NEW_FILE;
        addRealmAndURL( "testPutRequest_Anonymous", url, "", "" );
        ServerResponse resp =
            S2IOFacade.put( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url, null, monitor,
                            "Monitor name" );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_CREATED, resp.getStatusCode() );
        assertRequest( "Unexpected recorded request", "PUT", url );
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( S2IOFacadeAnonymousTest.class );
    }
}
