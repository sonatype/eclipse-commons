package org.maven.ide.eclipse.io;

import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.server.jetty.behaviour.Pause;
import org.sonatype.tests.http.server.jetty.configurations.DefaultSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.SslSuiteConfigurator;

@Configurators( { DefaultSuiteConfigurator.class, SslSuiteConfigurator.class } )
public class CustomTimeoutTest
    extends AbstractIOTest
{

    public void testNoTimeout_UseDefaultTimeout()
        throws Exception
    {
        tryHead( 28000, null, false ); // 28 seconds should be fine (default timeout is 30)
    }

    public void testTimeout_UseDefaultTimeout()
        throws Exception
    {
        tryHead( 35000, null, true ); // 35 seconds should be too much (default timeout is 30)
    }

    public void testNoTimeout_UseNonDefaultTimeout()
        throws Exception
    {
        tryHead( 65000, 70000, false );
    }

    public void testTimeout_UseNonDefaultTimeout()
        throws Exception
    {
        tryHead( 70000, 65000, true );
    }

    private void tryHead( long latency, Integer timeout, boolean expectTimeout )
        throws Exception
    {
        provider().addBehaviour( "/*", new Pause((int)latency) );
        long start = System.currentTimeMillis();
        try
        {
            S2IOFacade.head( server.getHttpUrl() + "/file.txt", timeout, monitor );
            long execTime = System.currentTimeMillis() - start;
            System.out.println( "Request succeeded in " + execTime + " ms" );
            if ( expectTimeout )
            {
                fail( "Expected timeout exception" );
            }
            assertTrue( "Request succeeded in " + execTime + " ms", execTime >= latency && execTime < latency + 2000 );
        }
        catch ( Exception e )
        {
            long execTime = System.currentTimeMillis() - start;
            System.out.println( "Request failed in " + execTime + " ms" );
            if ( !expectTimeout )
            {
                throw e;
            }
            if ( !isTimeoutException( e ) )
            {
                throw e;
            }
            // We got the expected timeout exception
            if ( timeout == null )
            {
                timeout = 30000; // The default timeout
            }
            assertTrue( "Request failed in " + execTime + " ms", execTime >= timeout && execTime < timeout + 2000 );
        }
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
    
    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( CustomTimeoutTest.class );
    }
}
