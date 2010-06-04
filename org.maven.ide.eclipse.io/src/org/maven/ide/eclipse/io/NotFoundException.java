package org.maven.ide.eclipse.io;

import java.io.IOException;

/**
 * 
 * http response 404
 * @author mkleint
 *
 */
public class NotFoundException
    extends IOException
{
    private static final long serialVersionUID = -3931433246316614538L;

    public NotFoundException( String message )
    {
        super( message );
    }

}
