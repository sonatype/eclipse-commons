package org.maven.ide.eclipse.authentication;

import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.maven.ide.eclipse.authentication.internal.AuthRegistry;
import org.maven.ide.eclipse.authentication.internal.AuthRegistryStates;
import org.maven.ide.eclipse.authentication.internal.SimpleAuthService;

public class AuthFacade
{
    private static volatile AuthRegistry authRegistry = null;

    private static AuthRegistry loadAuthRegistry()
    {
        if ( authRegistry != null )
        {
            return authRegistry;
        }

        authRegistry = new AuthRegistry();
        return authRegistry;
    }

    public static IAuthService getAuthService()
    {
        synchronized ( AuthRegistry.lock )
        {
            if ( AuthRegistry.getState() == AuthRegistryStates.LOADING )
            {
                // The request for auth service is made from the same thread that loads the registry,
                // so auth data is needed for the loading of the registry itself.
                return new SimpleAuthService( SecurePreferencesFactory.getDefault() );
            }

            return loadAuthRegistry();
        }
    }

    public static IAuthRegistry getAuthRegistry()
    {
        return loadAuthRegistry();
    }
}
