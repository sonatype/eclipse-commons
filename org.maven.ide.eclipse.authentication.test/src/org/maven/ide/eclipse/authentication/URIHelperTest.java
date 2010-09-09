package org.maven.ide.eclipse.authentication;

import junit.framework.TestCase;

import org.maven.ide.eclipse.authentication.internal.URIHelper;

public class URIHelperTest
    extends TestCase
{
    public void testURINormalizers()
    {
        String sUri = "http://foo";
        assertEquals( sUri, URIHelper.normalize( sUri ).toString() );
        assertEquals( sUri, URIHelper.normalize( TestURINormalizer.URI_PREFIX + sUri ).toString() );
    }

    public void testRemoveScmPrefix()
    {
        String sUri = "http://foo";
        assertEquals( sUri, URIHelper.normalize( sUri ).toString() );
        assertEquals( sUri, URIHelper.normalize( URIHelper.SCM_PREFIX + "svn:" + sUri ).toString() );
        assertEquals( sUri, URIHelper.normalize( URIHelper.SCM_PREFIX + "git:" + sUri ).toString() );
        try
        {
            assertEquals( sUri, URIHelper.normalize( URIHelper.SCM_PREFIX + sUri ).toString() );
            fail( "Expected InvalidURIException" );
        }
        catch ( InvalidURIException expected )
        {
            if ( !"SCM URI 'scm:http://foo' does not specify SCM type".equals( expected.getMessage() ) )
            {
                throw expected;
            }
        }
    }
}
