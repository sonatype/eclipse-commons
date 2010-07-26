package org.maven.ide.eclipse.authentication;

public class TestURINormalizer
    implements IURINormalizer
{
    public static final String URI_PREFIX = "unittest:";

    public boolean accept( String sUri )
    {
        return sUri.startsWith( URI_PREFIX );
    }

    public String normalize( String sUri )
    {
        if ( sUri.startsWith( URI_PREFIX ) )
        {
            return sUri.substring( URI_PREFIX.length() );
        }
        return sUri;
    }
}
