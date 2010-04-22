package org.maven.ide.eclipse.log.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogListener
    implements ILogListener
{

    public void logging( IStatus status, String plugin )
    {
        String name = status.getPlugin();
        if ( name == null )
        {
            name = plugin;
        }

        Logger logger = LoggerFactory.getLogger( name );

        Log log;
        switch ( status.getSeverity() )
        {
            case IStatus.ERROR:
                log = ERROR;
                break;
            case IStatus.WARNING:
                log = WARN;
                break;
            default:
                log = INFO;
                break;
        }

        if ( !log.isEnabled( logger ) )
        {
            return;
        }

        log( log, logger, status );
    }

    private void log( Log log, Logger logger, IStatus status )
    {
        if ( status == null )
        {
            return;
        }

        Throwable exception = status.getException();

        log.log( logger, status.getMessage(), exception );

        if ( exception instanceof CoreException )
        {
            log( log, logger, ( (CoreException) exception ).getStatus() );
        }

        if ( status.isMultiStatus() )
        {
            IStatus[] children = status.getChildren();
            if ( children != null )
            {
                for ( int i = 0; i < children.length; i++ )
                {
                    log( log, logger, children[i] );
                }
            }
        }
    }

    private static interface Log
    {

        boolean isEnabled( Logger logger );

        void log( Logger logger, String message, Throwable exception );

    }

    private static final Log ERROR = new Log()
    {

        public boolean isEnabled( Logger logger )
        {
            return logger.isErrorEnabled();
        }

        public void log( Logger logger, String message, Throwable exception )
        {
            logger.error( message, exception );
        }

    };

    private static final Log WARN = new Log()
    {

        public boolean isEnabled( Logger logger )
        {
            return logger.isWarnEnabled();
        }

        public void log( Logger logger, String message, Throwable exception )
        {
            logger.warn( message, exception );
        }

    };

    private static final Log INFO = new Log()
    {

        public boolean isEnabled( Logger logger )
        {
            return logger.isInfoEnabled();
        }

        public void log( Logger logger, String message, Throwable exception )
        {
            logger.info( message, exception );
        }

    };

}
