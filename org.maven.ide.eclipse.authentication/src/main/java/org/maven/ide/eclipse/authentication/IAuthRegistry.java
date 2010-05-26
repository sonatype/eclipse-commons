package org.maven.ide.eclipse.authentication;

import java.net.URISyntaxException;

public interface IAuthRegistry
{
    /** Returns a realm with a given id */
    public IAuthRealm getRealm( String realmId );

    /** Creates a realm for a given url/id if it doesn't exist and returns it (or a matching existing realm) */
    public IAuthRealm addRealm( String id );

    /** Creates a realm if it doesn't exist, maps it to a given URL and returns it (or a matching existing realm) */
    public IAuthRealm addRealm( String realmId, String url );

    /**
     * Removes the realm with the given id.
     * 
     * @return Returns the removed realm or null if the given id was not in the list of known realms
     */
    public IAuthRealm removeRealm( String id );

    /**
     * Removes all realms from the registry.
     */
    public void clear();

    /**
     * Maps the specified realm id to the realm for the given URL (if any).
     * 
     * @param realmId The realm id, must not be {@code null}.
     * @param url The URL whose realm should be mapped to the realm id, must not be {@code null}.
     * @throws URISyntaxException If the specified URL is incorrect
     */
    public void mapUrlToRealm( String realmId, String url )
        throws URISyntaxException;

}
