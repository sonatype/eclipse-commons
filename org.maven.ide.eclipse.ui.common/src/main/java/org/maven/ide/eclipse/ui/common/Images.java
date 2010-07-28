package org.maven.ide.eclipse.ui.common;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class Images
{
    public static final Image ARTIFACT = getImage( "artifact.gif" );

    public static final Image GROUP = getImage( "group.gif" );

    public static final Image VERSION = getImage( "version.gif" );

    private Images()
    {
    }

    public static Image getImage( String image )
    {
        return Activator.getDefault().getImage( image );
    }

    public static ImageDescriptor getImageDescriptor( String image )
    {
        return Activator.getDefault().getImageDescriptor( image );
    }
}
