package org.maven.ide.eclipse.ui.common.authentication;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthRegistry;
import org.maven.ide.eclipse.authentication.ISecurityRealmURLAssoc;

public class RealmUrlCollector
{
    private List<ISecurityRealmURLAssoc> associations;

    public RealmUrlCollector()
    {
        associations = new ArrayList<ISecurityRealmURLAssoc>();
    }

    public void save( IProgressMonitor monitor )
    {
        if ( associations.size() > 0 )
        {
            SubMonitor subMonitor =
                SubMonitor.convert( monitor, "Saving URL to realm associations...", associations.size() + 1 );

            IAuthRegistry registry = AuthFacade.getAuthRegistry();

            for ( ISecurityRealmURLAssoc assoc : associations )
            {
                registry.addURLToRealmAssoc( assoc.getUrl(), assoc.getRealmId(), assoc.getAnonymousAccess(), subMonitor );
                subMonitor.worked( 1 );
            }
            associations.clear();
        }
    }

    public void collect( RealmComposite realmComposite )
    {
        if ( realmComposite.isDirty() )
        {
            associations.add( realmComposite.getSecurityRealmURLAssoc() );
            realmComposite.clearDirty();
        }
    }

    public boolean isEmpty()
    {
        return associations.isEmpty();
    }
}
