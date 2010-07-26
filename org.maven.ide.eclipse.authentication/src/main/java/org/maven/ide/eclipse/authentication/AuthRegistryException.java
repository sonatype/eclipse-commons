package org.maven.ide.eclipse.authentication;


public class AuthRegistryException
    extends RuntimeException
{
    private static final long serialVersionUID = -5478280761254370765L;

    public AuthRegistryException( String message ) {
        super( message );
    }

    public AuthRegistryException( Exception cause ) {
        super( cause.getMessage(), cause );
    }

    public AuthRegistryException( String message, Exception cause ) {
        super( message, cause );
    }
}
