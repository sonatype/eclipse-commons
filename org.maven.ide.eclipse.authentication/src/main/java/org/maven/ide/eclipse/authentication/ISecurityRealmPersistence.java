package org.maven.ide.eclipse.authentication;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface ISecurityRealmPersistence
{
    Set<IAuthRealm> getRealms( IProgressMonitor monitor )
        throws CoreException;

    void addRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws CoreException;

    void updateRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws CoreException;

    void deleteRealm( String realmId, IProgressMonitor monitor )
        throws CoreException;
}
