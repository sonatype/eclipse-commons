package org.maven.ide.eclipse.swtvalidation;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE = Messages.class.getName().toLowerCase();

    static
    {
        NLS.initializeMessages( BUNDLE, Messages.class );
    }
    
    public static String ERR_Coordinate_Invalid;


}
