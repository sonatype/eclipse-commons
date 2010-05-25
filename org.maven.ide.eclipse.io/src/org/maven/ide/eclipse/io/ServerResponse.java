package org.maven.ide.eclipse.io;

public class ServerResponse
{

    private final int statusCode;

    private final byte[] responseData;

    private final String responseEncoding;

    public ServerResponse( int statusCode, byte[] responseData, String responseEncoding )
    {
        this.statusCode = statusCode;
        this.responseData = ( responseData != null ) ? responseData : new byte[0];
        this.responseEncoding = responseEncoding;
    }

    /**
     * Gets the status code returned by the server.
     * 
     * @return The status code returned by the server.
     */
    public int getStatusCode()
    {
        return statusCode;
    }

    /**
     * Gets the response data returned by the server.
     * 
     * @return The response data returned by the server, can be empty but never {@code null}.
     */
    public byte[] getResponseData()
    {
        return responseData;
    }

    /**
     * Gets the encoding of the response data returned by the server.
     * 
     * @return The encoding of the response data returned by the server or {@code null} if unknown.
     */
    public String getResponseEncoding()
    {
        return responseEncoding;
    }

}
