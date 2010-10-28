package org.maven.ide.eclipse.io;

import junit.framework.TestSuite;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSuiteConfigurator;

@Configurators( { BasicAuthSuiteConfigurator.class, DigestAuthSuiteConfigurator.class,
    BasicAuthSslSuiteConfigurator.class, DigestAuthSslSuiteConfigurator.class } )
public class AuthUrlPublisherTest
    extends AbstractIOTest
{
    private UrlPublisher publisher;
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        publisher = new UrlPublisher();
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( UrlPublisherTest.class );
    }

    public void testHttpPutUnauthorized()
        throws Exception
    {
        URI url = URI.create( server.getHttpUrl() + SECURE_FILE );
        try
        {
            publisher.putFile( new FileRequestEntity( new File( RESOURCES, FILE_LOCAL ) ), url,
                               new NullProgressMonitor(), AuthFacade.getAuthService(), null );
            fail( "UnauthorizedException should be thrown." );
        }
        catch ( UnauthorizedException e )
        {
            e.printStackTrace();
            assertTrue( e.getMessage(), e.getMessage().contains( String.valueOf( HttpURLConnection.HTTP_UNAUTHORIZED ) ) );
        }
    }

}
