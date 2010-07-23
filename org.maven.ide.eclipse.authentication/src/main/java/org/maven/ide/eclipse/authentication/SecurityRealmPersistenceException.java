package org.maven.ide.eclipse.authentication;


public class SecurityRealmPersistenceException
    extends RuntimeException
{
    private static final long serialVersionUID = -5086974132952511761L;

    public SecurityRealmPersistenceException( Exception cause ) {
        super( cause.getMessage(), cause );
    }

}
