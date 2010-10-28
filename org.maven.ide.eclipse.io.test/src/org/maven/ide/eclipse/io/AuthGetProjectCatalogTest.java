package org.maven.ide.eclipse.io;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;
import org.sonatype.tests.http.runner.annotations.Configurators;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.BasicAuthSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSslSuiteConfigurator;
import org.sonatype.tests.http.server.jetty.configurations.DigestAuthSuiteConfigurator;

@Configurators( { BasicAuthSuiteConfigurator.class, BasicAuthSslSuiteConfigurator.class,
    DigestAuthSuiteConfigurator.class, DigestAuthSslSuiteConfigurator.class } )
public class AuthGetProjectCatalogTest
    extends GetProjectCatalogTest
{
    
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        addRealmAndURL( "test", url(), "user", "password" );
    }

    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite(AuthGetProjectCatalogTest.class);
    }

}
