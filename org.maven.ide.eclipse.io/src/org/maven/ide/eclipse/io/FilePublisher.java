package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;


class FilePublisher
{

    /**
     * Uploads a file to the specified local path.
     * 
     * @param file The file to upload, must not be {@code null}.
     * @param destination The destination for the uploaded file, must not be {@code null}.
     * @param monitor The monitor to notify of transfer progress, may be {@code null}.
     * @throws IOException If the resource could not be uploaded.
     */
    public void putFile( final RequestEntity file, final File destination, final IProgressMonitor monitor )
        throws IOException
    {
        InputStream is = file.getContent();

        MonitoredInputStream mis = new MonitoredInputStream( is, monitor );
        mis.setName( "Writing file " + file.toString() );
        mis.setLength( (int) file.getContentLength() );

        try
        {
            destination.getParentFile().mkdirs();

            OutputStream os = new FileOutputStream( destination );
            try
            {
                byte[] buffer = new byte[1024 * 8];
                while ( true )
                {
                    int read = mis.read( buffer, 0, buffer.length );
                    if ( read < 0 )
                    {
                        break;
                    }
                    os.write( buffer, 0, read );
                }
            }
            finally
            {
                try
                {
                    os.close();
                }
                catch ( IOException e )
                {
                    // ignored
                }
            }
        }
        finally
        {
            try
            {
                mis.close();
            }
            catch ( IOException e )
            {
                // ignored
            }
        }
    }

}
