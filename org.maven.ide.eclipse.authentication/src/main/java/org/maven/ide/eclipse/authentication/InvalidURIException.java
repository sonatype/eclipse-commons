package org.maven.ide.eclipse.authentication;

import java.net.URISyntaxException;

public class InvalidURIException
    extends RuntimeException
{
    private static final long serialVersionUID = -1021079089366959755L;

    public InvalidURIException( URISyntaxException cause )
    {
        super( cause.getMessage(), cause );
    }

    public InvalidURIException( String message )
    {
        super( message );
    }
}
