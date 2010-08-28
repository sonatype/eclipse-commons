package org.maven.ide.eclipse.io;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;

public abstract class AbstractIOTest
    extends TestCase
{
    protected static final IProgressMonitor monitor = new NullProgressMonitor();

    protected static final String FILE_PATH = "/file.txt";

    protected static final String SECURE_FILE = "/secured/secure.txt";

    protected static final String AUTH_TYPE = "Basic ";

    @Override
    public void tearDown()
        throws Exception
    {
        AuthFacade.getAuthRegistry().clear();
    }

    /*
     * Read a stream
     */
    protected static String readstream( InputStream stream )
        throws IOException
    {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[128];
        int size = -1;
        while ( ( size = stream.read( buffer ) ) == 128 )
        {
            builder.append( new String( buffer, 0, size ) );
        }
        stream.close();
        if ( size != -1 )
            builder.append( new String( buffer, 0, size ) );
        return builder.toString();
    }

    protected static void addRealmAndURL( String realmId, String url, AuthenticationType type,
                                          AnonymousAccessType anonType )
    {
        IAuthRealm realm = AuthFacade.getAuthRegistry().addRealm( realmId, realmId, realmId, type, monitor );
        AuthFacade.getAuthRegistry().addURLToRealmAssoc( url, realm.getId(), anonType, monitor );
    }

    protected static void addRealmAndURL( String realmId, String url, String username, String password )
    {
        addRealmAndURL( realmId, url, AuthenticationType.USERNAME_PASSWORD, AnonymousAccessType.ALLOWED );
        AuthFacade.getAuthService().save( url, username, password );
    }
}
