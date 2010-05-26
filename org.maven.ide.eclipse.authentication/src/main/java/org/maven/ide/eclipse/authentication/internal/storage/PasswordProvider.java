package org.maven.ide.eclipse.authentication.internal.storage;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordProvider
    extends org.eclipse.equinox.security.storage.provider.PasswordProvider
{
    private final static Logger log = LoggerFactory.getLogger( PasswordProvider.class );

    private static PasswordProviderDelegate delegate;

    @Override
    public PBEKeySpec getPassword( IPreferencesContainer container, int passwordType )
    {
        PasswordProviderDelegate delegate = getDelegate();

        if ( delegate != null )
        {
            return delegate.getPassword( container, passwordType );
        }

        return null;
    }

    @Override
    public boolean retryOnError( Exception e, IPreferencesContainer container )
    {
        PasswordProviderDelegate delegate = getDelegate();

        if ( delegate != null )
        {
            return delegate.retryOnError( e, container );
        }

        return false;
    }

    public static synchronized void setDelegate( PasswordProviderDelegate delegate )
    {
        if ( delegate != null && PasswordProvider.delegate != null )
        {
            throw new IllegalStateException();
        }
        PasswordProvider.delegate = delegate;
    }

    private synchronized static PasswordProviderDelegate getDelegate()
    {
        if ( delegate != null )
        {
            return delegate;
        }

        IExtensionRegistry registry = RegistryFactory.getRegistry();
        IExtensionPoint point = registry.getExtensionPoint( "com.sonatype.s2.authentication.passwordProviderDelegate" );
        IExtension[] extensions = point.getExtensions();

        if ( extensions.length != 1 )
        {
            throw new IllegalStateException( "There should be one and only one password privder delegate" );
        }

        try
        {
            return (PasswordProviderDelegate) extensions[0].getConfigurationElements()[0].createExecutableExtension( "class" );
        }
        catch ( CoreException e )
        {
            log.error( "Could not instantiate required component", e );
        }

        return null;
    }
}
