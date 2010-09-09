package org.maven.ide.eclipse.authentication.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.maven.ide.eclipse.authentication.AuthRegistryException;
import org.maven.ide.eclipse.authentication.IURINormalizer;
import org.maven.ide.eclipse.authentication.InvalidURIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URIHelper
{
    private static final Logger log = LoggerFactory.getLogger( URIHelper.class );

    public static final String SCM_PREFIX = "scm:";

    private static List<IURINormalizer> uriNormalizers = null;

    public static URI normalize( String sUri )
    {
        sUri = callURINormalizers( sUri );

        sUri = removeScmPrefix( sUri );
        sUri = sUri.replace( '\\', '/' );
        try
        {
            URI uri = new URI( sUri ).normalize();
            return uri;
        }
        catch ( URISyntaxException e )
        {
            throw new InvalidURIException( e );
        }
    }

    private static String callURINormalizers( String sUri )
    {
        try
        {
            List<IURINormalizer> applicableURINormalizers = new ArrayList<IURINormalizer>();
            for ( IURINormalizer uriNormalizer : getURINormalizers() )
            {
                if ( uriNormalizer.accept( sUri ) )
                {
                    log.debug( "Found applicable URI normalizer {} for URI {}", uriNormalizer.getClass(), sUri );
                    applicableURINormalizers.add( uriNormalizer );
                }
            }
            for ( IURINormalizer uriNormalizer : applicableURINormalizers )
            {
                sUri = uriNormalizer.normalize( sUri );
                log.debug( "URI normalizer {} result: {}", uriNormalizer.getClass(), sUri );
            }
            return sUri;
        }
        catch ( URISyntaxException e )
        {
            throw new InvalidURIException( e );
        }
    }

    public static String removeScmPrefix( String sUri )
    {
        if ( sUri == null )
        {
            return null;
        }

        if ( !sUri.startsWith( SCM_PREFIX ) )
        {
            return sUri;
        }
        String result = sUri.substring( SCM_PREFIX.length() );
        int at = result.indexOf( ':' );
        int at1 = result.indexOf( "://" );
        if ( at <= 0 || at == at1 )
        {
            throw new InvalidURIException( "SCM URI '" + sUri + "' does not specify SCM type" );
        }
        result = result.substring( at + 1 );
        return result;
    }

    private static List<IURINormalizer> getURINormalizers()
    {
        if ( uriNormalizers != null )
        {
            return uriNormalizers;
        }

        uriNormalizers = new ArrayList<IURINormalizer>();
        IConfigurationElement[] extensionConfigurationElements =
            Platform.getExtensionRegistry().getConfigurationElementsFor( IURINormalizer.EXTENSION_POINT_ID );
        for ( IConfigurationElement extensionConfigurationElement : extensionConfigurationElements )
        {
            Object o;
            try
            {
                o = extensionConfigurationElement.createExecutableExtension( "class" );
            }
            catch ( CoreException e )
            {
                throw new AuthRegistryException( "Error creating executable extension for URI normalizer: "
                    + e.getMessage(), e );
            }
            log.debug( "Found URI normalizer: {}", o.getClass().getCanonicalName() );
            if ( o instanceof IURINormalizer )
            {
                IURINormalizer normalizer = (IURINormalizer) o;
                uriNormalizers.add( normalizer );
            }
            else
            {
                throw new IllegalArgumentException( o.getClass().getCanonicalName()
                    + " does not implement the IURINormalizer interface." );
            }
        }
        return uriNormalizers;
    }
}
