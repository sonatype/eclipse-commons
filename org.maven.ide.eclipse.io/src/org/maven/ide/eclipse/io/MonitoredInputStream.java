package org.maven.ide.eclipse.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;

class MonitoredInputStream
    extends FilterInputStream
{

    private final IProgressMonitor monitor;

    private volatile String name = "";

    private volatile int length = IProgressMonitor.UNKNOWN;

    private volatile Throwable exception;

    private boolean started;

    public MonitoredInputStream( InputStream is, IProgressMonitor monitor )
    {
        super( is );

        this.monitor = monitor;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setLength( int length )
    {
        this.length = length;
    }

    public void setException( Throwable exception )
    {
        if ( this.exception == null )
        {
            this.exception = exception;
        }
    }

    @Override
    public int read()
        throws IOException
    {
        checkForCancel();

        int c = super.read();

        checkForError();

        if ( monitor != null )
        {
            if ( !started )
            {
                monitor.beginTask( name, length );
            }
            if ( c < 0 )
            {
                monitor.done();
            }
            else
            {
                monitor.worked( 1 );
            }
        }

        started = true;

        return c;
    }

    @Override
    public int read( byte[] b, int off, int len )
        throws IOException
    {
        checkForCancel();

        int n = super.read( b, off, len );

        checkForError();

        if ( monitor != null )
        {
            if ( !started )
            {
                monitor.beginTask( name, length );
            }
            if ( n < 0 )
            {
                monitor.done();
            }
            else
            {
                monitor.worked( n );
            }
        }

        started = true;

        return n;
    }

    private void checkForCancel()
        throws IOException
    {
        if ( monitor != null && monitor.isCanceled() )
        {
            throw new IOException( "Transfer has been canceled" );
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
