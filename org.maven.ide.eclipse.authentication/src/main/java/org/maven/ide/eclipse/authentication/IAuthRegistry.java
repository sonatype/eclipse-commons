package org.maven.ide.eclipse.authentication;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IAuthRegistry
{
    /** Returns a realm with a given id or null if no such realm exists */
    public IAuthRealm getRealm( String realmId );

    /**
     * Creates a new security realm. If a persistence mechanism for security realms is available, the new realm is
     * persisted. If a realm with the provided id already exists,
     * 
     * @param id The id for the new security realm. Must not be null.
     * @param name The name for the new security realm. Must not be null.
     * @param description The name for the new security realm. May be null.
     * @param authenticationType The type of authentication used for the realm.
     * @return The newly created security realm.
     * @throws AuthRegistryException If a realm with the provided id already exists or the persistence mechanism cannot
     *             persist the new realm.
     */
    public IAuthRealm addRealm( String id, String name, String description, AuthenticationType authenticationType,
                                IProgressMonitor monitor );

    /**
     * Removes the realm with the given id.
     * 
     * @return Returns the removed realm or null if the given id was not in the list of known realms
     */
    public IAuthRealm removeRealm( String id );

    /**
     * @param url The URL to associate. Must not be null.
     * @param realmId The id of the realm to associate. Must not be null.
     * @param anonymousAccessType The type of access for anonymous. Must not be null.
     * @return The newly created URL to realm association.
     */
    public ISecurityRealmURLAssoc addURLToRealmAssoc( String url, String realmId,
                                                      AnonymousAccessType anonymousAccessType, IProgressMonitor monitor );

    IAuthRealm getRealmForURI( String uri );

    public void clear();

    public void reload( IProgressMonitor monitor );
}
