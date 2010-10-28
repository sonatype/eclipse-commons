package org.maven.ide.eclipse.io;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSuiteConfigurator;

@Configurators( { BasicAuthSuiteConfigurator.class, DigestAuthSuiteConfigurator.class,
    BasicAuthSslSuiteConfigurator.class, DigestAuthSslSuiteConfigurator.class } )
public class RedirectAuthTest
    extends RedirectTest
{

    private int i = 0;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        String username = "user";
        String password = "password";
        String realm = "Test Server";

        String[] urls =
            { url( "redirect", NEW_FILE ), url( "redirect", FILE_PATH ), url( FILE_PATH ), url( NEW_FILE ) };
        for ( String url : urls )
        {
            addRealmAndURL( realm + ( i++ ), url, username, password );
        }

    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( RedirectAuthTest.class );
    }
}
