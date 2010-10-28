package org.maven.ide.eclipse.io;

import junit.framework.TestSuite;

import org.sonatype.tests.http.runner.junit.Junit3SuiteConfiguration;

public class RedirectProjectCatalogTest
    extends AbstractIOTest
{
    
    public void testDummy()
    {
        
    }
    
    public static TestSuite suite()
        throws Exception
    {
        return Junit3SuiteConfiguration.suite( RedirectProjectCatalogTest.class );
    }

}
