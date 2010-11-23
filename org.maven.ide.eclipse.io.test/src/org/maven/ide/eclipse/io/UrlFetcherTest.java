package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.DefaultSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.SslSuiteConfigurator;
import org.sonatype.tests.http.server.api.ServerProvider;

@Configurators( { DefaultSuiteConfigurator.class, SslSuiteConfigurator.class } )
public class UrlFetcherTest
    extends AbstractIOTest
{
    private UrlFetcher fetcher;

	@Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        fetcher = new UrlFetcher();
    }

    /*
     * Tests the error thrown when a file is not found.
     */
    public void testHttpOpenstreamFileNotFound()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + "/nonExistentFile" );
        try
        {
            readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
            fail( "NotFoundException should be thrown." );
        }
        catch ( NotFoundException e )
        {
            assertTrue( e.getMessage(), e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_NOT_FOUND ) ) );
        }
    }

    /*
     * Tests the error thrown when a user does not have permission to access a file.
     */
    public void testHttpOpenstreamForbidden()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + SECURE_FILE );
        try
        {
            readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
            fail( "UnauthorizedException should be thrown." );
        }
        catch ( UnauthorizedException e )
        {
            assertTrue( e.getMessage(), e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_UNAUTHORIZED ) ) );
        }
    }

    /*
     * Tests the contents of a file remote file
     */
    public void testHttpOpenStream()
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
    public void testHttpUsernameAndPasswordSent()
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
    public void testHttpUsernameOnly()
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
    public void testHttpPasswordOnly()
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
    public void testHttpAnonymous()
        throws Exception
    {
        URI address = URI.create( server.getHttpUrl() + FILE_PATH );
        addRealmAndURL( "testAnonymous", address.toString(), "", "" );
        readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        assertNull( "No Auth header should be set",
                    server.getRecordedHeaders( FILE_PATH ).get( "Authorization" ) );
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

    @Override
    public void configureProvider( ServerProvider provider )
    {
        provider().addAuthentication( "/secured/*", "BASIC" );
        provider().addUser( VALID_USERNAME, PASSWORD );
        super.configureProvider( provider );
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( UrlFetcherTest.class );
    }
}
