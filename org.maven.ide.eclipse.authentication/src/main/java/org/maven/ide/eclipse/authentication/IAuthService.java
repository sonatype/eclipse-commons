package org.maven.ide.eclipse.authentication;

import java.io.File;
import java.net.URI;

/**
 * Provides authentication data for accessing protected resources.
 */
public interface IAuthService
{
    /**
     * Returns authentication data to access the specified resource.
     * 
     * @param uri The URI of the resource to access, must not be {@code null}.
     * @return The authentication data or {@code null} if the resource is not known.
     */
    IAuthData select( URI uri );

    /**
     * Returns authentication data to access the specified resource.
     * 
     * @param uri The URI of the resource to access, must not be {@code null}.
     * @return The authentication data or {@code null} if the resource is not known.
     */
    IAuthData select( String uri );

    /**
     * Saves username and password used to access the specified URI.
     */
    void save( String uri, String username, String password );

    void save( String uri, IAuthData authData );

    void save( String uri, File certificatePath, String certificatePassphrase );
}
