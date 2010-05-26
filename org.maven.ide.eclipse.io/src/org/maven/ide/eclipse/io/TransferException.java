package org.maven.ide.eclipse.io;

import java.io.IOException;

public class TransferException
    extends IOException
{
    private static final long serialVersionUID = -86740798872415684L;

    private final static byte[] NEXUS_RESPONSE_HTML = { '<', 'h', 't', 'm', 'l', '>' };

    private final ServerResponse response;

    private String nexusErrorText;

    private boolean nexusErrorResponse;

    public TransferException( String message, ServerResponse response, Throwable cause )
    {
        super( message );
        initCause( cause );

        this.response = response;
        parseNexusResponse();
    }

    public ServerResponse getServerResponse()
    {
        return response;
    }

    public boolean hasNexusError()
    {
        return nexusErrorResponse;
    }

    public String getNexusError()
    {
        return nexusErrorText;
    }

    private void parseNexusResponse()
    {
        if ( response != null )
        {
            byte[] data = response.getResponseData();

            if ( arrayStartsWith( data, NEXUS_RESPONSE_HTML ) )
            {
                // in case of an error, Nexus returns a piece of HTML
                // with the error text wrapped with <h3>...</h3>
                String s = new String( data );
                String[] split = s.split( "</*h3>" );
                if ( split.length > 1 )
                {
                    nexusErrorResponse = true;
                    nexusErrorText = split[1];
                }
            }
        }
    }

    private boolean arrayStartsWith( byte[] a1, byte[] a2 )
    {
        if ( a1 == a2 )
        {
            return true;
        }
        if ( a1 == null || a2 == null )
        {
            return false;
        }
        if ( a1.length < a2.length )
        {
            return false;
        }

        for ( int i = 0; i < a2.length; i++ )
        {
            if ( a1[i] != a2[i] )
            {
                return false;
            }
        }

        return true;
    }
}
