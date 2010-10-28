package org.maven.ide.eclipse.io;

import java.io.FileInputStream;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.DefaultSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.SslSuiteConfigurator;

@Configurators( { DefaultSuiteConfigurator.class, SslSuiteConfigurator.class } )
public class GetProjectCatalogTest
    extends AbstractIOTest
{
    public void testCatalog()
        throws Exception
    {
        String catalogUrl = server.getHttpUrl() + "/catalogs/basic/catalog.xml";

        /*
         * Request: GET /catalogs/basic/catalog.xml HTTP/1.1. Host: localhost:49805. Pragma: no-cache. Cache-Control:
         * no-cache, no-store. Accept-Encoding: gzip. Connection: keep-alive. Accept: *\/*. User-Agent: NING/1.0. .
         */
        String expected = readstream( new FileInputStream( "resources/catalogs/basic/catalog.xml" ) );
        String result = readstream( S2IOFacade.openStream( catalogUrl, new NullProgressMonitor() ) );
        assertEquals( expected, result );
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( GetProjectCatalogTest.class );
    }
}
