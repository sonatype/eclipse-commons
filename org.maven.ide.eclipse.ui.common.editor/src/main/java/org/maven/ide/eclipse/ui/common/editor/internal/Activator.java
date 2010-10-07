package org.maven.ide.eclipse.ui.common.editor.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator
    extends AbstractUIPlugin
{
    public static final String PLUGIN_ID = "org.maven.ide.eclipse.ui.common.editor";

    public static final String IMAGE_PATH = "icons/";

    private static Activator plugin;

    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );
        plugin = this;
    }

    public void stop( BundleContext context )
        throws Exception
    {
        plugin = null;
        super.stop( context );
    }

    public static Activator getDefault()
    {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     * 
     * @param path the path
     * @return the image descriptor
     */
    public ImageDescriptor getImageDescriptor( String path )
    {
        ImageRegistry imageRegistry = getImageRegistry();
        if ( imageRegistry != null )
        {
            ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( path );
            if ( imageDescriptor == null )
            {
                imageDescriptor = imageDescriptorFromPlugin( PLUGIN_ID, IMAGE_PATH + path );
                imageRegistry.put( path, imageDescriptor );
            }
            return imageDescriptor;
        }

        return null;
    }

    public Image getImage( String path )
    {
        ImageRegistry imageRegistry = getImageRegistry();
        if ( imageRegistry != null )
        {
            getImageDescriptor( path );
            return imageRegistry.get( path );
        }
        return null;
    }
}
