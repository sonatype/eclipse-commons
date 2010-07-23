package org.maven.ide.eclipse.authentication;

import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.maven.ide.eclipse.authentication.internal.AuthRealm;
import org.maven.ide.eclipse.authentication.internal.AuthRegistry;
import org.maven.ide.eclipse.authentication.internal.AuthRegistryStates;
import org.maven.ide.eclipse.authentication.internal.SimpleAuthService;

public class AuthFacade
{
    private static volatile AuthRegistry authRegistry = null;

    private static synchronized AuthRegistry loadAuthRegistry()
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
        if ( AuthRegistry.getState() == AuthRegistryStates.LOADING )
        {
            return new SimpleAuthService( SecurePreferencesFactory.getDefault() );
        }

        if ( authRegistry != null )
        {
            return authRegistry;
        }
        return loadAuthRegistry();
    }

    public static IAuthRegistry getAuthRegistry()
    {
        if ( authRegistry != null )
        {
            return authRegistry;
        }
        return loadAuthRegistry();
    }

    public static IAuthRealm newAuthRealm( String id, String name, String description,
                                           AuthenticationType authenticationType )
    {
        return new AuthRealm( id, name, description, authenticationType );
    }

    public static ISecurityRealmURLAssoc newSecurityRealmURLAssoc( String id, String url, String realmId,
                                                       AnonymousAccessType anonymousAccessType )
    {
        return new SecurityRealmURLAssoc( id, url, realmId, anonymousAccessType );
    }
}
