package org.maven.ide.eclipse.io;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;

public class S2IOFacadeLocalTest
    extends AbstractIOTest
{

    @Override
    public void setUp()
        throws Exception
    {
        // we do not need a server for local testing
    }

    public void testExists_Local()
        throws Exception
    {
        assertTrue( S2IOFacade.exists( new File( RESOURCES, FILE_LOCAL ).toURI().toString(), monitor ) );
    }

    public void testExists_Local_NotFound()
        throws Exception
    {
        assertFalse( S2IOFacade.exists( new File( RESOURCES, NEW_FILE ).toURI().toString(), monitor ) );
    }

    public void testHeadRequest_Local()
        throws Exception
    {
        URI address = new File( RESOURCES, FILE_LOCAL ).toURI();
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_OK, resp.getStatusCode() );
    }

    public void testHeadRequest_Local_NotFound()
        throws Exception
    {
        URI address = new File( RESOURCES, "missingfile.txt" ).toURI();
        ServerResponse resp = S2IOFacade.head( address.toString(), null, monitor );
        assertEquals( "Unexpected HTTP status code", HttpURLConnection.HTTP_NOT_FOUND, resp.getStatusCode() );
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( S2IOFacadeLocalTest.class );
    }
}
