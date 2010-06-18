package org.maven.ide.eclipse.swtvalidation;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class Images
{

    public static final Image ERROR = getImage( "error_ovr.gif" );
    public static final Image WARN = getImage( "warn_ovr.gif" );
    public static final Image INFO = getImage( "info_ovr.gif" );

    public static ImageDescriptor getImageDescriptor( String key )
    {
        try
        {
            ImageRegistry imageRegistry = getImageRegistry();
            if ( imageRegistry != null )
            {
                ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( key );
                if ( imageDescriptor == null )
                {
                    imageDescriptor = createDescriptor( key );
                    imageRegistry.put( key, imageDescriptor );
                }
                return imageDescriptor;
            }
        }
        catch ( Exception ex )
        {
//            log.error( key, ex );
        }
        return null;
    }

    public static ImageDescriptor createImageDescriptor( String key, ImageData imageData )
    {
        try
        {
            ImageRegistry imageRegistry = getImageRegistry();
            if ( imageRegistry != null )
            {
                ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( key );
                if ( imageDescriptor != null )
                {
                    imageRegistry.remove( key );
                }
                {
                    imageDescriptor = ImageDescriptor.createFromImageData( imageData );
                    imageRegistry.put( key, imageDescriptor );
                }
                return imageDescriptor;
            }
        }
        catch ( Exception ex )
        {
//            log.error( key, ex );
        }
        return null;
    }

    public static Image getImage( String key )
    {
        getImageDescriptor( key );
        ImageRegistry imageRegistry = getImageRegistry();
        return imageRegistry == null ? null : imageRegistry.get( key );
    }

    private static ImageRegistry getImageRegistry()
    {
        Activator plugin = Activator.getDefault();
        return plugin == null ? null : plugin.getImageRegistry();
    }

    private static ImageDescriptor createDescriptor( String image )
    {
        try
        {
            return ImageDescriptor.createFromURL( new URL( image ) );
        }
        catch ( MalformedURLException e )
        {
            return AbstractUIPlugin.imageDescriptorFromPlugin( Activator.PLUGIN_ID, "icons/" + image );
        }
    }
}
