package org.maven.ide.eclipse.authentication;

import java.net.URI;

/**
 * Provides authentication data for accessing protected resources.
 */
public interface IAuthService
{

    /**
     * Returns authentication data to access the specified resources.
     * 
     * @param uri The URI of the resource to access, must not be {@code null}.
     * @return The authentication data or {@code null} if the resource is unprotected.
     */
    IAuthData select( URI uri );

}
