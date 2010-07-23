package org.maven.ide.eclipse.authentication.internal;

import java.net.URI;
import java.net.URISyntaxException;

import org.maven.ide.eclipse.authentication.AuthRegistryException;

public class URIHelper
{
    private static final String SCM_PREFIX = "scm:";

    public static URI normalize( String sUri )
    {
        sUri = removeScmPrefix( sUri );
        sUri = sUri.replace( '\\', '/' );
        try
        {
            URI uri = new URI( sUri ).normalize();
            return uri;
        }
        catch ( URISyntaxException e )
        {
            throw new AuthRegistryException( e );
        }
    }

    public static String removeScmPrefix( String sUri )
    {
        if ( sUri == null )
        {
            return null;
        }

        if ( !sUri.startsWith( SCM_PREFIX ) )
        {
            return sUri;
        }
        String result = sUri.substring( SCM_PREFIX.length() );
        int at = result.indexOf( ':' );
        int at1 = result.indexOf( "://" );
        if ( at <= 0 || at == at1 )
        {
            throw new AuthRegistryException( "SCM URI '" + sUri + "'does not specify SCM type" );
        }
        result = result.substring( at + 1 );
        return result;
    }
}
