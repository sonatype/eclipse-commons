package org.maven.ide.eclipse.io;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.behaviour.Content;
import org.sonatype.tests.http.server.jetty.behaviour.Pause;
import org.sonatype.tests.http.server.jetty.behaviour.Record;
import org.sonatype.tests.http.server.jetty.configurations.DefaultSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.SslSuiteConfigurator;
import org.sonatype.tests.http.server.api.ServerProvider;
import java.util.concurrent.TimeoutException;

@Configurators( { DefaultSuiteConfigurator.class, SslSuiteConfigurator.class } )
public class TimeoutTest
    extends AbstractIOTest
{

    @Override
    public void configureProvider( ServerProvider provider )
    {
        recorder = new Record();
        provider().addBehaviour( "/*", recorder, new Pause(), new Content( "someContent" ) );
        server = new ServerProviderWrapper( provider() );
    }

    public void testUrlFetcherNoTimeout()
        throws IOException
    {
        int pause = 28000;
        String url = url( String.valueOf(pause), "doesnotmatter" );

        UrlFetcher fetcher = new UrlFetcher();
        URI address = URI.create( url );
        long start = System.currentTimeMillis();
        String content =
            readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
        long time = System.currentTimeMillis() - start;
        assertRequest( "request missing", "GET", url );
        assertEquals( "wrong response body", "someContent", content );
        assertTrue("Request needed " + time + "ms", time >= pause && time < pause+2000);
    }

    public void testUrlFetcherTimeout()
        throws IOException
    {
        int defaultTimeout = 30000;
        int pause = 32000;
        String url = url( String.valueOf(pause), "doesnotmatter" );

        UrlFetcher fetcher = new UrlFetcher();
        URI address = URI.create( url );
        long start = 0;
        try
        {
            start = System.currentTimeMillis();
            readstream( fetcher.openStream( address, new NullProgressMonitor(), AuthFacade.getAuthService(), null ) );
            fail( "Expected IOException (timeout)" );
        }
        catch ( Exception e )
        {
            long time = System.currentTimeMillis() - start;
            assertTrue("Request needed " + time + "ms", time >= defaultTimeout && time < defaultTimeout+2000);
            assertRequest( "request missing", "GET", address.toURL().toString() );
            assertTrue( "failure was not caused by timeout", isTimeoutException( e ));
        }
    }

    public void testS2IOFacade_Head()
        throws IOException, URISyntaxException
    {
        String url = url( "2000", "doesnotmatter" );
        int timeout = 1000;

        long start = 0;
        try
        {
            start = System.currentTimeMillis();
            S2IOFacade.head( url, timeout, new NullProgressMonitor() );
            fail( "Expected IOException (timeout)" );
        }
        catch ( Exception e )
        {
            long time = System.currentTimeMillis() - start;
            assertTrue("Request needed " + time + "ms", time >= timeout && time < timeout+800);
            assertRequest( "request missing", "HEAD", url );
            assertTrue( "failure was not caused by timeout", isTimeoutException( e ));
        }
    }

    public void testS2IOFacade_Delete()
        throws IOException, URISyntaxException
    {
        String url = url( "2000", "doesnotmatter" );

        int timeout = 1000;
        long start = 0;
        try
        {
            start = System.currentTimeMillis();
            S2IOFacade.delete( url, timeout, new NullProgressMonitor(), "" );
            fail( "Expected IOException (timeout)" );
        }
        catch ( Exception e )
        {
            long time = System.currentTimeMillis() - start;
            assertTrue("Request needed " + time + "ms", time >= timeout && time < timeout+800);
            assertRequest( "request missing", "DELETE", url );
            assertTrue( "failure was not caused by timeout", isTimeoutException( e ));
        }
    }

    public void testS2IOFacade_Put()
        throws IOException, URISyntaxException
    {
        String url = url( "2000", "doesnotmatter" );

        int timeout = 1000;
        long start = 0;
        try
        {
            start = System.currentTimeMillis();
            S2IOFacade.put( new ByteArrayRequestEntity( new byte[] { 1, 2, 3, 4 }, "application/octet-stream" ), url,
                            timeout, new NullProgressMonitor() );
            fail( "Expected IOException (timeout)" );
        }
        catch ( Exception e )
        {
            long time = System.currentTimeMillis() - start;
            assertTrue("Request needed " + time + "ms", time >= timeout && time < timeout+800);
            assertRequest( "request missing", "PUT", url );
            assertTrue( "failure was not caused by timeout", isTimeoutException( e ));
        }
    }

    public void testS2IOFacade_Post()
        throws IOException, URISyntaxException
    {
        String url = url( "2000", "doesnotmatter" );

        int timeout = 1000;
        long start = 0;
        try
        {
            start = System.currentTimeMillis();
            S2IOFacade.post( new ByteArrayRequestEntity( new byte[] { 1, 2, 3, 4 }, "application/octet-stream" ), url,
                             1000, new NullProgressMonitor(), "" );
            fail( "Expected IOException (timeout)" );
        }
        catch ( Exception e )
        {
            long time = System.currentTimeMillis() - start;
            assertTrue("Request needed " + time + "ms", time >= timeout && time < timeout+800);
            assertRequest( "request missing", "POST", url );
            assertTrue( "failure was not caused by timeout", isTimeoutException( e ));
        }
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( TimeoutTest.class );
    }
    
    public static boolean isTimeoutException( Throwable e )
    {
        while ( e != null )
        {
            if ( e instanceof TimeoutException )
            {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

}
