package org.maven.ide.eclipse.log.internal;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.maven.ide.eclipse.pr.IDataGatherer;
import org.maven.ide.eclipse.pr.IDataGatheringContext;
import org.maven.ide.eclipse.pr.IDataTarget;
import org.maven.ide.eclipse.pr.sources.ExternalFileSource;

public class LogDataGatherer
    implements IDataGatherer
{

    public void gather( IDataGatheringContext context )
        throws CoreException
    {
        String logDir = System.getProperty( LogPlugin.PROPERTY_LOG_DIRECTORY, "" );
        if ( logDir.length() <= 0 )
        {
            return;
        }

        File dir = new File( logDir );
        File[] logs = dir.listFiles();
        if ( logs != null )
        {
            IDataTarget target = context.getTarget();
            for ( File log : logs )
            {
                target.consume( "logs", new ExternalFileSource( log.getAbsolutePath(), log.getName() ) );
            }
        }
    }

    public String getName()
    {
        return "Maven Logs";
    }

}
