package org.maven.ide.eclipse.log.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogPlugin
    extends Plugin
{

    private final LogListener logListener = new LogListener();

    public static final String PROPERTY_LOG_DIRECTORY = "org.maven.ide.eclipse.log.dir";

    public static final String PROPERTY_LOG_FILE = "org.maven.ide.eclipse.log.file";

    @Override
    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );

        configureLogger();

        Platform.addLogListener( logListener );
    }

    private void configureLogger()
    {
        if ( System.getProperty( ContextInitializer.CONFIG_FILE_PROPERTY ) != null )
        {
            return;
        }

        File stateDir = getStateLocation().toFile();

        File configFile = new File( stateDir, "logback.xml" );

        if ( !configFile.isFile() )
        {
            try
            {
                InputStream is = getClass().getResourceAsStream( "/jars/logback.xml" );
                try
                {
                    configFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream( configFile );
                    try
                    {
                        for ( byte[] buffer = new byte[1024 * 4];; )
                        {
                            int n = is.read( buffer );
                            if ( n < 0 )
                            {
                                break;
                            }
                            fos.write( buffer, 0, n );
                        }
                    }
                    finally
                    {
                        fos.close();
                    }
                }
                finally
                {
                    is.close();
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                return;
            }
        }

        try
        {
            if ( System.getProperty( PROPERTY_LOG_DIRECTORY, "" ).length() <= 0 )
            {
                System.setProperty( PROPERTY_LOG_DIRECTORY, stateDir.getAbsolutePath() );
            }

            configureLogger( configFile.toURL() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public static void configureLogger( URL configFile )
        throws JoranException
    {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext( lc );
        configurator.doConfigure( configFile );

        StatusPrinter.printInCaseOfErrorsOrWarnings( lc );
    }

    @Override
    public void stop( BundleContext context )
        throws Exception
    {
        Platform.removeLogListener( logListener );

        super.stop( context );
    }

}
