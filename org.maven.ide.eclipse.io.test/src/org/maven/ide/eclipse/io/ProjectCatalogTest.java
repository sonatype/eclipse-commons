package org.maven.ide.eclipse.io;

import java.io.FileInputStream;
import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;


/**
 * These tests are derived from com.sonatype.s2.project.integration.test/.../ProjectCatalogTest,
 * but with the intermediate APIs cut out, so we can test the async-http-client more closely.
 * 
 * @author rgould
 */
public class ProjectCatalogTest extends AbstractIOTest {

    /**
     * Tests catalog retrieval via HTTP protocol when catalog URL points at directory.
     */
    public void testCatalogOverHttp()
        throws Exception
    {
//         // startHttpServer();
        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";

        /*
         * Request: GET /catalogs/basic/catalog.xml HTTP/1.1. Host: localhost:49805. Pragma: no-cache. Cache-Control:
         * no-cache, no-store. Accept-Encoding: gzip. Connection: keep-alive. Accept: *\/*. User-Agent: NING/1.0. .
         */
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTP protocol and BASIC authentication.
     */
    public void testCatalogOverHttpWithBasicAuth()
        throws Exception
    {
//         // startHttpServer();
        // server.addUser( "testuser", "testpass", "authorized" );
        // server.addSecuredRealm( "/*", "authorized" );
        // server.start();

        addRealmAndURL( "test", url(), "testuser", "testpass" );

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";

        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTP protocol and BASIC authentication when the password is empty.
     */
    public void testCatalogOverHttpWithBasicAuthUsingEmptyPassword()
        throws Exception
    {
//         // startHttpServer();
        // server.addUser( "testuser", "", "authorized" );
        // server.addSecuredRealm( "/*", "authorized" );
        // server.start();
        //
        // addRealmAndURL( "test", server.getHttpUrl(), "testuser", "" );

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTPS protocol.
     */
    public void testCatalogOverHttps()
        throws Exception
    {
//         // startHttpServer();
        //
        // String catalogUrl = server.getHttpsUrl() + "/catalogs/basic/catalog.xml";
        String catalogUrl = null;
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTPS protocol and BASIC authentication.
     */
    public void testCatalogOverHttpsWithBasicAuth()
        throws Exception
    {
//         // startHttpServer();
        // server.addUser( "testuser", "testpass", "authorized" );
        // server.addSecuredRealm( "/*", "authorized" );
        // server.start();
        //
        // addRealmAndURL( "test", server.getHttpsUrl(), "testuser", "testpass" );
        //
        // String catalogUrl = server.getHttpsUrl() + "/catalogs/basic/catalog.xml";
        String catalogUrl = null;
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTPS after redirection from HTTP protocol.
     */
    public void testCatalogOverHttpsAfterRedirectionFromHttp()
        throws Exception
    {
        // newHttpServer();
        // server.setRedirectToHttps( true );
        // server.start();

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTP when the server hangs.
     */
    public void testCatalogOverHttpWhenServerTimeouts()
        throws Exception
    {

        // newHttpServer();
        // server.setLatency( 60 * 60 * 1000 );
        // server.start();

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";

        try
        {
            readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
            fail( "Exception not thrown" );
        }
        catch ( Exception e )
        {
            assertEquals(e.getClass(), TimeoutException.class);
        }
    }

    /**
     * Tests catalog retrieval via HTTP when the host is bad.
     */
    public void testCatalogOverHttpWhenHostIsBad()
        throws Exception
    {
        // server.start();

        String catalogUrl = "http://bad.host/catalogs/basic";
        try
        {
            readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
            fail( "Exception not thrown" );
        }
        catch ( ConnectException e )
        {
            assertTrue( e.getCause() != null );
            assertTrue( e.getCause() instanceof UnresolvedAddressException );
        }
    }

    /**
     * Tests catalog retrieval via HTTP proxy.
     */
    public void testCatalogOverHttpProxy()
        throws Exception
    {
        // setProxy( "localhost", server.getHttpPort(), false, null, null );

        String catalogUrl = "http://host-to-be-proxied.org/catalogs/basic/catalog.xml";
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTP proxy that requires authentication.
     */
    public void testCatalogOverHttpProxyWithProxyAuth()
        throws Exception
    {
        // newHttpServer();
        // server.setProxyAuth( "proxyuser", "proxypass" );
        // server.start();
        //
        // setProxy( "localhost", server.getHttpPort(), false, "proxyuser", "proxypass" );

        String catalogUrl = "http://host-to-be-proxied.org/catalogs/basic/catalog.xml";
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    /**
     * Tests catalog retrieval via HTTP when a non-applicable proxy is present.
     */
    public void testCatalogOverHttpWithNonProxiedHost()
        throws Exception
    {
        setProxy( "bad.host", 12347, false, null, null );
        setNonProxiedHosts( "localhost" );

        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }
    
    public static TestSuite suite()
        throws Exception
    {
    	return Junit3SuiteConfiguration.suite(ProjectCatalogTest.class);
    }
    
}
