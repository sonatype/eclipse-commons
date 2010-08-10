package org.maven.ide.eclipse.ui.common;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class Images
{
    public static final Image ARTIFACT = getImage( "artifact.gif" ); //$NON-NLS-1$

    public static final Image AUTH_REALM = getImage( "authrealm.png" ); //$NON-NLS-1$

    public static final Image GROUP = getImage( "group.gif" ); //$NON-NLS-1$

    public static final ImageDescriptor REFRESH_DESCRIPTOR = getImageDescriptor( "refresh.gif" ); //$NON-NLS-1$

    public static final Image VERSION = getImage( "version.gif" ); //$NON-NLS-1$

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
