package org.maven.ide.eclipse.io;

import java.io.IOException;

import org.maven.ide.eclipse.tests.common.HttpServer;

public class TimeoutTest
    extends AbstractIOTest
{
    protected void startHttpServer( long latency )
        throws Exception
    {
        // The server is stopped in super.tearDown
        server = new HttpServer();
        server.addResources( "/", "resources" );
        server.setLatency( latency );
        server.start();
    }

    public void testNoTimeout_UseDefaultTimeout()
        throws Exception
    {
        tryHead( 28000, null ); // 28 seconds should be fine (default timeout is 30)
    }

    public void testTimeout_UseDefaultTimeout()
        throws Exception
    {
        try
        {
            tryHead( 35000, null ); // 32 seconds should be too much (default timeout is 30)
            fail("Request is expected to time out");
        }
        catch ( IOException e )
        {
            assertTrue( e.getMessage().toLowerCase().contains("connection timed out") );
        }
    }

    public void testNoTimeout_UseNonDefaultTimeout()
        throws Exception
    {
        tryHead( 63000, 65000 );
    }

    public void testTimeout_UseNonDefaultTimeout()
        throws Exception
    {
        try
        {
            tryHead( 65000, 63000 );
            fail( "Request is expected to time out" );
        }
        catch ( IOException e )
        {
            assertTrue( e.getMessage().toLowerCase().contains( "connection timed out" ) );
        }
    }

    private void tryHead( long latency, Integer timeout )
        throws Exception
    {
        startHttpServer( latency );
        long start = System.currentTimeMillis();
        try
        {
            S2IOFacade.head( server.getHttpUrl() + "/file.txt", timeout, monitor );
            long execTime = System.currentTimeMillis() - start;
            System.out.println( "Request finished in " + execTime + " ms" );
            assertTrue( "Request finished in " + execTime + " ms", execTime >= latency && execTime < latency + 2000 );
        }
        catch ( IOException e )
        {
            long execTime = System.currentTimeMillis() - start;
            if ( !e.getMessage().toLowerCase().contains( "connection timed out" ) )
            {
                throw e;
            }
            if ( timeout == null )
            {
                timeout = 30000; // The default timeout
            }
            System.out.println( "Request failed in " + execTime + " ms" );
            assertTrue( "Request failed in " + execTime + " ms", execTime >= timeout && execTime < timeout + 2000 );
            throw e;
        }
        finally
        {
            System.out.println( "Request finished after " + ( System.currentTimeMillis() - start ) + " ms" );
        }
    }
}
