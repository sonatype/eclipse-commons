package org.maven.ide.eclipse.authentication;

import org.maven.ide.eclipse.authentication.internal.AuthRealm;
import org.maven.ide.eclipse.authentication.internal.AuthRegistry;

public class AuthFacade
{
    private static AuthRegistry authRegistry = new AuthRegistry();

    public static IAuthService getAuthService()
    {
        return authRegistry;
    }

    public static IAuthRegistry getAuthRegistry()
    {
        return authRegistry;
    }

    public static IAuthRealm newAuthRealm(String id, String name, String description)
    {
        return new AuthRealm( id, name, description );
    }
}
