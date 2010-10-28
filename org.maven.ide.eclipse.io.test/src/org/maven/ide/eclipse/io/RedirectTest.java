package org.maven.ide.eclipse.io;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.DefaultSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.SslSuiteConfigurator;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.api.ServerProvider;

@Configurators( { DefaultSuiteConfigurator.class, SslSuiteConfigurator.class } )
public class RedirectTest
    extends AbstractIOTest
{

    protected static String NEW_FILE = AbstractIOTest.NEW_FILE.substring( 1 );

    protected static String FILE_PATH = AbstractIOTest.FILE_PATH.substring( 1 );

    public class RedirectPattern
        implements Behaviour
    {

        private final String pattern;

        private final String replace;

        public RedirectPattern( String pattern, String replace )
        {
            this.pattern = pattern;
            this.replace = replace;
        }

        public boolean execute( HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx )
            throws Exception
        {
            String path = request.getPathInfo();
            path = path.replaceAll( pattern, replace );

            response.sendRedirect( path );

            return false;
        }

    }

    @Override
    public void configureProvider( ServerProvider provider )
    {
        super.configureProvider( provider );
        provider().addBehaviour( "/redirect/*", recorder, new RedirectPattern( "^/redirect/", "/" ) );
    }

    public void testGet()
        throws IOException, URISyntaxException
    {
        String url = url( "redirect", FILE_PATH );
        String content = readstream( S2IOFacade.openStream( url, monitor ) );
        assertRequest( "Did not follow redirect: ", "GET", url( FILE_PATH ) );
        assertEquals( "contents", content.trim() );
    }
    
    public void testGet_NotFound()
        throws IOException, URISyntaxException
    {
        String url = url( "redirect", NEW_FILE );
        try
        {
            readstream( S2IOFacade.openStream( url, monitor ) );
            fail( "expected NotFoundException" );
        }
        catch ( NotFoundException e )
        {
            assertRequest( "Did not follow redirect: ", "GET", url( NEW_FILE ) );
        }
    }

    public void testPut_DoNotFollow()
        throws IOException, URISyntaxException
    {
        String url = url( "redirect", NEW_FILE );
        try {
            ByteArrayRequestEntity entity = new ByteArrayRequestEntity( new byte[] {1,2,3,4}, "application/octet-stream" );
            S2IOFacade.put( entity, url, monitor );
            fail( "expected TransferException (302)" );
        }
        catch ( TransferException e )
        {
            assertRequest( "did not request at all", "PUT", url );
        }
    }

    public void testPost_DoNotFollow()
        throws IOException, URISyntaxException
    {
        String url = url( "redirect", NEW_FILE );
        try
        {
            ByteArrayRequestEntity entity = new ByteArrayRequestEntity( new byte[] { 1, 2, 3, 4 }, "application/octet-stream" );
            S2IOFacade.post( entity, url, 1000, monitor, "testPost_DoNotFollow" );
            fail( "expected TransferException (302)" );
        }
        catch ( TransferException e )
        {
            assertRequest( "did not request at all", "POST", url );
        }
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( RedirectTest.class );
    }
}
