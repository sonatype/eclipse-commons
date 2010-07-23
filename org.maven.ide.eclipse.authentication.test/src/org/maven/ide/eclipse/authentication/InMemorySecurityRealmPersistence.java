package org.maven.ide.eclipse.authentication;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

public class InMemorySecurityRealmPersistence
    extends AbstractSecurityRealmPersistence
    implements ISecurityRealmPersistence, IExecutableExtension
{
    private static Map<String, IAuthRealm> realms = new LinkedHashMap<String, IAuthRealm>();

    private static Map<String, ISecurityRealmURLAssoc> urls = new LinkedHashMap<String, ISecurityRealmURLAssoc>();

    private Random rand = new Random( System.currentTimeMillis() );

    private String generateId()
    {
        return Long.toHexString( System.nanoTime() + rand.nextInt( 2008 ) );
    }

    public void addRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        realms.put( realm.getId(), realm );
    }

    public void deleteRealm( String realmId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        realms.remove( realmId );
    }

    public Set<IAuthRealm> getRealms( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        Set<IAuthRealm> result = new LinkedHashSet<IAuthRealm>();
        for ( IAuthRealm realm : realms.values() )
        {
            result.add( realm );
        }
        return result;
    }

    public void updateRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        realms.put( realm.getId(), realm );
    }

    public ISecurityRealmURLAssoc addURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc,
                                                      IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        securityRealmURLAssoc =
            new SecurityRealmURLAssoc( generateId(), securityRealmURLAssoc.getUrl(),
                                       securityRealmURLAssoc.getRealmId(), securityRealmURLAssoc.getAnonymousAccess() );
        urls.put( securityRealmURLAssoc.getId(), securityRealmURLAssoc );
        return securityRealmURLAssoc;
    }

    public void deleteURLToRealmAssoc( String urlAssocId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        urls.remove( urlAssocId );
    }

    public Set<ISecurityRealmURLAssoc> getURLToRealmAssocs( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        Set<ISecurityRealmURLAssoc> result = new LinkedHashSet<ISecurityRealmURLAssoc>();
        for ( ISecurityRealmURLAssoc url : urls.values() )
        {
            result.add( url );
        }
        return result;
    }

    public void updateURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        urls.put( securityRealmURLAssoc.getId(), securityRealmURLAssoc );
    }
}
