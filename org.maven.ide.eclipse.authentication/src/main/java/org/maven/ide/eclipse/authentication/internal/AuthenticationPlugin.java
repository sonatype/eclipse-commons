package org.maven.ide.eclipse.authentication.internal;


import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class AuthenticationPlugin
    extends Plugin
{
    // The plug-in ID
    public static final String PLUGIN_ID = "com.sonatype.s2.authentication";

    private Logger log = LoggerFactory.getLogger( AuthenticationPlugin.class );

    @Override
    public void start( BundleContext context )
        throws Exception
    {
        log.debug( "Starting the AuthenticationPlugin..." );
        super.start( context );
    }

    @Override
    public void stop( BundleContext context )
        throws Exception
    {
        log.debug( "Stoping the AuthenticationPlugin..." );
        super.stop( context );
    }
}
