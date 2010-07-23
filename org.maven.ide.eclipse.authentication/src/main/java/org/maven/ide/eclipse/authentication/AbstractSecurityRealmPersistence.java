package org.maven.ide.eclipse.authentication;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

public abstract class AbstractSecurityRealmPersistence
    implements ISecurityRealmPersistence, IExecutableExtension
{
    private int priority = Integer.MAX_VALUE;

    public int getPriority()
    {
        return priority;
    }

    // IExecutableExtension
    public void setInitializationData( IConfigurationElement config, String propertyName, Object data )
    {
        String priority = config.getAttribute( ISecurityRealmPersistence.ATTR_PRIORITY );

        this.priority = Integer.parseInt( priority );
    }

    private boolean active = false;

    public boolean isActive()
    {
        return active;
    }

    public void setActive( boolean active )
    {
        this.active = active;
    }
}
