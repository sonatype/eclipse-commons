package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;


class FileFetcher
{

    /**
     * Opens a stream to the specified file.
     * 
     * @param file The file to open, must not be {@code null}.
     * @param monitor The monitor to notify of transfer progress, may be {@code null}.
     * @return The input stream to the specified file, never {@code null}.
     * @throws IOException If the file could not be opened.
     */
    public InputStream openStream( final File file, final IProgressMonitor monitor )
        throws IOException
    {
        InputStream is = new FileInputStream( file );

        MonitoredInputStream mis = new MonitoredInputStream( is, monitor );
        mis.setName( "Reading file " + file.getAbsolutePath() );
        mis.setLength( (int) file.length() );

        return mis;
    }

}
