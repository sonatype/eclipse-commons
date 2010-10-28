package org.maven.ide.eclipse.io;

import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.DefaultSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.SslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.impl.JettyServerProvider;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.api.ServerProvider;

@Configurators( { DefaultSuiteConfigurator.class, SslSuiteConfigurator.class } )
public class RedirectCustomTest
    extends AbstractIOTest
{

    static class PortRedirector
        implements Behaviour
    {

        public boolean execute( HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx )
            throws Exception
        {
            String path = request.getPathInfo().substring( 1 );
            String[] split = path.split( "/", 2 );
            String target = URLDecoder.decode( split[0], "UTF-8" );

            String newPath = target + "/" + split[1];

            response.sendRedirect( newPath );

            return false;
        }

    }

    private JettyServerProvider p;

    @Override
    public void configureProvider( ServerProvider provider )
    {
        super.configureProvider( provider );
        provider.addBehaviour( "/redirect/*", new PortRedirector() );
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        p = new JettyServerProvider();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        if ( p != null )
        {
            p.stop();
            p = null;
        }
    }

    public void testRedirectToHttp()
        throws Exception
    {
        p.addDefaultServices();
        p.start();

        String url = url( "redirect", p.getUrl().toString(), "content", "foo" );
        String content = readstream( S2IOFacade.openStream( url, monitor ) );
        assertEquals( "foo", content.trim() );
    }

    public void testRedirectToHttps()
        throws Exception
    {
        p.setSSL( "keystore", "password" );
        p.addDefaultServices();
        p.start();

        String url = url( "redirect", p.getUrl().toString(), "content", "foo" );
        String content = readstream( S2IOFacade.openStream( url, monitor ) );
        assertEquals( "foo", content.trim() );
    }

    public void testRedirectWithAuth()
        throws Exception
    {
        p.addAuthentication( "/*", "BASIC" );
        p.addUser( "user", "password" );
        p.addDefaultServices();
        p.start();

        String url = url( "redirect", p.getUrl().toString(), "content", "foo" );
        addRealmAndURL( "testRedirectWithAuth", url, "user", "password" );
        String content = readstream( S2IOFacade.openStream( url, monitor ) );
        assertEquals( "foo", content.trim() );
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( RedirectCustomTest.class );
    }
}
