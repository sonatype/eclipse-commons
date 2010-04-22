package org.maven.ide.eclipse.log;

import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;

public class LogHelper
{
    public static void logJavaProperties( Logger log )
    {
        Properties javaProperties = System.getProperties();
        SortedMap<String, String> sortedProperties = new TreeMap<String, String>();
        for ( Object key : javaProperties.keySet() )
        {
            sortedProperties.put( (String) key, (String) javaProperties.get( key ) );
        }
        log.info( "Java properties:" );
        for ( String key : sortedProperties.keySet() )
        {
            log.info( "   {}={}", key, sortedProperties.get( key ) );
        }
    }
}
