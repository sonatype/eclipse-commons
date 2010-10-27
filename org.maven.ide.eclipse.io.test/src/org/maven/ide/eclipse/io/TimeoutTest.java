package org.maven.ide.eclipse.io;

import java.io.IOException;

import org.maven.ide.eclipse.tests.common.HttpServer;

public class TimeoutTest
    extends AbstractIOTest
{
    protected void startHttpServer( long latency )
        throws Exception
    {
        server = new HttpServer();
        server.addResources( "/", "resources" );
        server.setLatency( latency );
        server.start();
    }

    public void testNoTimeout()
        throws Exception
    {
        tryHead( 28000 ); // 28 seconds should be fine (timeout is 30)
    }

    public void testTimeout()
        throws Exception
    {
        try
        {
            tryHead( 32000 ); // 32 seconds should be too much (timeout is 30)
            fail("Request is expected to time out");
        }
        catch ( IOException e )
        {
            assertTrue( e.getMessage().toLowerCase().contains("connection timed out") );
        }
    }

    private void tryHead( long latency )
        throws Exception
    {
        startHttpServer( latency );
        S2IOFacade.head( server.getHttpUrl() + "/file.txt", null, monitor );
        server.stop();
    }
}
