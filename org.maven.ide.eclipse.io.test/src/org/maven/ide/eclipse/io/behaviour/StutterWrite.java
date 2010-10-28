package org.maven.ide.eclipse.io.behaviour;

import java.io.FileOutputStream;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.jetty.behaviour.filesystem.FSBehaviour;

public class StutterWrite
    extends FSBehaviour
{

    private final int pause;

    public StutterWrite( String fsPath, int i )
    {
        super( fsPath );
        this.pause = i;
    }

    public boolean execute( HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx )
        throws Exception
    {
        boolean handled = false;

        int code = 200;
        if ( !fs( request.getPathInfo() ).exists() )
        {
            code = 201;
        }

        if ( "PUT".equals( request.getMethod() ) || "POST".equals( request.getMethod() ) )
        {
            handled = true;

            FileOutputStream out = null;
            try
            {
                out = new FileOutputStream( fs( request.getPathInfo() ) );
                ServletInputStream in = request.getInputStream();
                int b = -1;
                while ( ( b = in.read() ) != -1 )
                {
                    out.write( b );
                    Thread.sleep( pause );
                }
            }
            finally
            {
                if ( out != null )
                {
                    out.close();
                }
            }

        }

        if ( handled )
        {
            response.setStatus( code );
        }

        return !handled;
    }
}