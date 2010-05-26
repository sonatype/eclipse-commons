package org.maven.ide.eclipse.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteArrayRequestEntity
    implements RequestEntity
{
    private final byte[] data;

    private final String contentType;

    private String name = "";

    public ByteArrayRequestEntity( byte[] data, String contentType )
    {
        this.data = data;
        this.contentType = contentType;
    }

    public InputStream getContent()
        throws IOException
    {
        return new ByteArrayInputStream( data );
    }

    public String getContentType()
    {
        return contentType;
    }

    public long getContentLength()
    {
        return data.length;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }
}
