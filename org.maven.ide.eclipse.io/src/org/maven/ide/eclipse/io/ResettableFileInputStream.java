package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;

class ResettableFileInputStream
    extends FilterInputStream
{

    private final File file;

    public ResettableFileInputStream( File file )
        throws IOException
    {
        super( new FileInputStream( file ) );
        this.file = file;
    }

    @Override
    public boolean markSupported()
    {
        return true;
    }

    @Override
    public synchronized void reset()
        throws IOException
    {
        try
        {
            this.in.close();
        }
        catch ( IOException e )
        {
            // ignore
        }

        this.in = new FileInputStream( file );
    }

    @Override
    public int available()
        throws IOException
    {
        // enable proper progess feedback
        return 0;
    }

}