package org.maven.ide.eclipse.io.internal;


import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class S2IOPlugin
    extends Plugin
{
    // The plug-in ID
    public static final String PLUGIN_ID = "com.sonatype.s2.io";

    private static S2IOPlugin plugin;

    private Logger log = LoggerFactory.getLogger( S2IOPlugin.class );

    private ServiceTracker proxyServiceTracker;

    @Override
    public void start( BundleContext context )
        throws Exception
    {
        log.debug( "Starting the S2IOPlugin..." );
        super.start( context );
        plugin = this;

        proxyServiceTracker = new ServiceTracker( context, IProxyService.class.getName(), null );
        proxyServiceTracker.open();
    }

    @Override
    public void stop( BundleContext context )
        throws Exception
    {
        log.debug( "Stoping the S2IOPlugin..." );
        try
        {
            proxyServiceTracker.close();
            proxyServiceTracker = null;
        }
        finally
        {
            plugin = null;
            super.stop( context );
        }
    }

    /**
     * Gets the proxy service if available.
     * 
     * @return The proxy service or {@code null} if not available.
     */
    public IProxyService getProxyService()
    {
        return (IProxyService) proxyServiceTracker.getService();
    }

    public static S2IOPlugin getDefault()
    {
        return plugin;
    }
}
