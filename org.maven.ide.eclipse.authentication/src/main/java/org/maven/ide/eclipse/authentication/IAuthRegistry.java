package org.maven.ide.eclipse.authentication;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IAuthRegistry
{
    /** @return The realm with the given id or null if no such realm exists. */
    IAuthRealm getRealm( String realmId );

    /** @return All realms or empty collection if there are no known realms. The returned collection is not modifiable. */
    Collection<IAuthRealm> getRealms();

    /**
     * Creates a new security realm. If a persistence mechanism for security realms is available, the new realm is
     * persisted.
     * 
     * @param id The id for the new security realm. Must not be null.
     * @param name The name for the new security realm. Must not be null.
     * @param description The name for the new security realm. May be null.
     * @param authenticationType The type of authentication used for the realm.
     * @return The newly created security realm.
     * @throws AuthRegistryException If a realm with the provided id already exists or the persistence mechanism cannot
     *             persist the new realm.
     */
    IAuthRealm addRealm( String id, String name, String description, AuthenticationType authenticationType,
                                IProgressMonitor monitor );

    /**
     * Updates a security realm. If a persistence mechanism for security realms is available, the realm is persisted.
     * 
     * @param authRealm The realm to be updated. Must not be null.
     * @throws AuthRegistryException If the realm does not exist or the persistence mechanism cannot persist the realm.
     */
    void updateRealm( IAuthRealm authRealm, IProgressMonitor monitor );

    /**
     * Removes the realm with the given id. If a persistence mechanism for security realms is available, the realm is
     * removed from persistent storage.
     * 
     * @throws AuthRegistryException If a realm with the provided id does not exist or the persistence mechanism cannot
     *             delete the realm.
     */
    void removeRealm( String id, IProgressMonitor monitor );

    /**
     * @return The URL to realm association with the given id or null if no such association exists.
     */
    ISecurityRealmURLAssoc getURLToRealmAssoc( String urlToRealmAssocId );

    /**
     * @return All URL to realm associations or empty collection if there are no known associations. The returned
     *         collection is not modifiable.
     */
    Collection<ISecurityRealmURLAssoc> getURLToRealmAssocs();

    /**
     * @param url The URL to associate. Must not be null.
     * @param realmId The id of the realm to associate. Must not be null.
     * @param anonymousAccessType The type of access for anonymous. Must not be null.
     * @return The newly created URL to realm association.
     */
    ISecurityRealmURLAssoc addURLToRealmAssoc( String url, String realmId,
                                                      AnonymousAccessType anonymousAccessType, IProgressMonitor monitor );

    /**
     * Updates a URL to realm association. If a persistence mechanism for security realms is available, the association
     * is persisted.
     * 
     * @param urlToRealmAssoc The URL to realm association to be updated. Must not be null.
     * @throws AuthRegistryException If the URL to realm association does not exist or the persistence mechanism cannot
     *             persist the association.
     */
    void updateURLToRealmAssoc( ISecurityRealmURLAssoc urlToRealmAssoc, IProgressMonitor monitor );

    /**
     * Removes the URL to realm association with the given id. If a persistence mechanism for security realms is
     * available, the association is removed from persistent storage.
     * 
     * @throws AuthRegistryException If a URL to realm association with the provided id does not exist or the
     *             persistence mechanism cannot delete the association.
     */
    void removeURLToRealmAssoc( String urlToRealmAssocId, IProgressMonitor monitor );

    IAuthRealm getRealmForURI( String uri );

    void clear();

    void reload( IProgressMonitor monitor );
}
