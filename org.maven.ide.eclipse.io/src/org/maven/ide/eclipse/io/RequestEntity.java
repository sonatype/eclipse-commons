package org.maven.ide.eclipse.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Name shamelessly copied from org.apache.commons.httpclient.methods.RequestEntity
 * 
 * @author igor
 */
public interface RequestEntity
{
    public InputStream getContent()
        throws IOException;

    public String getContentType();

    public long getContentLength();

    public String getName();
}
