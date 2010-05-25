package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileRequestEntity
    implements RequestEntity
{
    private final File file;

    private String contentType;

    public FileRequestEntity( File file )
    {
        this.file = file;
    }

    public InputStream getContent()
        throws IOException
    {
        return new ResettableFileInputStream( file );
    }

    public String getContentType()
    {
        return contentType;
    }

    public long getContentLength()
    {
        return file.length();
    }

    public String getName()
    {
        return file.toString();
    }
}
