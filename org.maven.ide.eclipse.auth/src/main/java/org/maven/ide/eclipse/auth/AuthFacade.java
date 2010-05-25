package org.maven.ide.eclipse.auth;

import org.maven.ide.eclipse.auth.internal.AuthRegistry;

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

}
