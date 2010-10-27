package org.maven.ide.eclipse.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;

public class MonitoredOutputStream
    extends FilterOutputStream
{

    private IProgressMonitor monitor;

    private int length = IProgressMonitor.UNKNOWN;

    private Throwable exception;

    private boolean started = false;

    private String name = "";

    public MonitoredOutputStream( OutputStream out, IProgressMonitor monitor )
    {
        super( out );

        this.monitor = monitor;
    }

    /**
     * The total length to be used for the progress monitor. Callers may use IProgressMonitor.UNKNOWN for unknown
     * lengths.
     * 
     * @param length
     */
    public void setLength( int length )
    {
        this.length = length;
    }

    /**
     * Sets the exception for this stream. If an exception is present when write() is called, it will be thrown. This
     * exception can only be set once. Subsequent calls will be ignored.
     * 
     * @param exception to exception to be thrown if write() is called
     */
    public void setException( Throwable exception )
    {
        if ( this.exception == null )
        {
            this.exception = exception;
        }
    }
    
    /**
     * Sets the name to be used as the task for the progress monitor.
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Returns the cancelled state of the progress monitor
     * 
     * @return true if the progress monitor has been cancelled. False otherwise.
     */
    public boolean isCancelled()
    {
        return monitor.isCanceled();
    }
    
    @Override
    public void write( int b )
        throws IOException
    {
        checkForCancel();
        
        super.write( b );
        
        checkForError();
        
        if ( monitor != null )
        {
            if ( !started )
            {
                started = true;
                monitor.beginTask( name, length );
            }
            monitor.worked( 1 );
        }
    }
    
    private void checkForCancel()
        throws IOException
    {
        if ( monitor != null && monitor.isCanceled() )
        {
            throw new IOException( "Transfer has been cancelled" );
        }
    }

    private void checkForError()
        throws IOException
    {
        if ( exception != null )
        {
            if ( exception instanceof IOException )
            {
                throw (IOException) exception;
            }
            else
            {
                throw (IOException) new IOException( exception.getMessage() ).initCause( exception );
            }
        }
    }
}
