package org.maven.ide.eclipse.authentication;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

public interface ISecurityRealmPersistence
{
    String EXTENSION_POINT_ID = "org.maven.ide.eclipse.authentication.SecurityRealmPersistence";

    String ATTR_PRIORITY = "priority";

    public int getPriority();

    Set<IAuthRealm> getRealms( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    void addRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    void updateRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    void deleteRealm( String realmId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    Set<ISecurityRealmURLAssoc> getURLToRealmAssocs( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    ISecurityRealmURLAssoc addURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc,
                                          IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    void updateURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    void deleteURLToRealmAssoc( String urlAssocId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException;

    /**
     * @return true only if this instance is the one used by the AuthRegistry for security realm persistence.
     */
    boolean isActive();

    /**
     * Used by the AuthRegistry to indicate that this instance is the one used by the AuthRegistry for security realm
     * persistence.
     */
    void setActive( boolean active );
}
