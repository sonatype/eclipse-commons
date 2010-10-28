package org.maven.ide.eclipse.io;



import junit.framework.TestSuite;

import org.maven.ide.eclipse.io.behaviour.StutterRead;
import org.maven.ide.eclipse.io.behaviour.StutterWrite;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.server.jetty.behaviour.Record;
import org.sonatype.tests.http.server.jetty.behaviour.filesystem.Delete;
import org.sonatype.tests.http.server.api.ServerProvider;

public class StutteringTest
    extends S2IOFacadeAnonymousTest
{

    public void configureProvider( ServerProvider provider )
    {
        recorder = new Record();
        String fsPath = "resources";
        provider().addBehaviour( "/*", recorder, new StutterRead( fsPath, 1000 ), new StutterWrite( fsPath, 1000 ),
                                 new Delete( fsPath ) );
        server = new ServerProviderWrapper( provider() );
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( StutteringTest.class );
    }
}
