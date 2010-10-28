package org.maven.ide.eclipse.io.behaviour;

import java.io.FileInputStream;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.jetty.behaviour.filesystem.FSBehaviour;

public class StutterRead
    extends FSBehaviour
{

    private final int pause;

    public StutterRead( String fsPath, int i )
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
            code = 404;
        }
        if ( "HEAD".equals( request.getMethod() ) )
        {
            handled = true;
        }
        else if ( "GET".equals( request.getMethod() ) )
        {
            handled = true;
            if ( fs( request.getPathInfo() ).canRead() )
            {
                FileInputStream in = null;
                try
                {
                    in = new FileInputStream( fs( request.getPathInfo() ) );
                    ServletOutputStream out = response.getOutputStream();
                    int b = -1;
                    while ( ( b = in.read() ) != -1 )
                    {
                        out.write( b );
                        Thread.sleep( pause );
                    }
                }

                finally
                {
                    if ( in != null )
                    {
                        in.close();
                    }
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