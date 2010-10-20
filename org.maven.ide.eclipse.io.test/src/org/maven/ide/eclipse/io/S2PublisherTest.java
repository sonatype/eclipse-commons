package org.maven.ide.eclipse.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.tests.common.HttpServer;

public class S2PublisherTest
    extends AbstractIOTest
{
    private static final String PROJECT_REPOSITORY_PATH = "/content/repositories/nx-codebase-repo";

    private static final String PROJECT_DESCRIPTOR_FILENAME = "mse-codebase.xml";

    private static final String PROJECT_ICON_FILENAME = "mse-codebase-icon.png";

    private static final String PROJECT_PREFERENCES_FILENAME = "mse-codebase-preferences.jar";

    protected int timeout_ms = 1000 * 10;

    protected File tmpDir;

    @Override
    protected HttpServer newHttpServer()
        throws Exception
    {
        super.newHttpServer();
        tmpDir = File.createTempFile( "catalog", "" );
        tmpDir.delete();
        server.addResources( "catalog", tmpDir.getAbsolutePath() );
        server.addSecuredRealm( "/catalog/secured/*", VALID_USERNAME );
        return server;
    }

    private static void copyDirectory( File sourceLocation, File targetLocation )
        throws IOException
    {

        if ( sourceLocation.isDirectory() )
        {
            if ( !targetLocation.exists() )
            {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for ( int i = 0; i < children.length; i++ )
            {
                copyDirectory( new File( sourceLocation, children[i] ), new File( targetLocation, children[i] ) );
            }
        }
        else
        {

            InputStream in = new FileInputStream( sourceLocation );
            OutputStream out = new FileOutputStream( targetLocation );

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ( ( len = in.read( buf ) ) > 0 )
            {
                out.write( buf, 0, len );
            }
            in.close();
            out.close();
        }
    }

    private File getTmpDir( String name )
    {
        return new File( tmpDir, name + PROJECT_REPOSITORY_PATH );
    }

    public void testPublishProjectWithoutIcon()
        throws Exception
    {
        newHttpServer().start();
        // HEAD /catalog/content/repositories/nx-codebase-repo/test/projectWithoutIcon/1.0.0/
        // 404 Not Found

        String projectBase =
            server.getHttpUrl() + "/catalog/content/repositories/nx-codebase-repo/test/projectWithoutIcon/1.0.0/";
        ServerResponse response = S2IOFacade.head( projectBase, timeout_ms, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithoutIcon/1.0.0/mse-codebase.xml
        // 201 Created

        String putURL = projectBase + "mse-codebase.xml";
        String bodyInput = "resources/projects/projectWithoutIcon/mse/mse-codebase.xml";

        response = S2IOFacade.put( new FileRequestEntity( new File( bodyInput ) ), putURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        File file = new File( getTmpDir( "" ), "test/projectWithoutIcon/1.0.0/" + PROJECT_DESCRIPTOR_FILENAME );
        assertTrue( file.exists() );
        assertEquals( readstream( new FileInputStream( file ) ), readstream( new FileInputStream( bodyInput ) ) );
        assertEquals( 1, new File( getTmpDir( "" ), "test/projectWithoutIcon/1.0.0/" ).list().length );
    }

    public void testPublishProjectWithIcon()
        throws Exception
    {
        newHttpServer().start();

        // HEAD /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/
        // 404 Not Found

        String projectBase =
            server.getHttpUrl() + "/catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/";
        ServerResponse response = S2IOFacade.head( projectBase, timeout_ms, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/mse-codebase.xml
        // 201 Created

        String putURL = projectBase + "mse-codebase.xml";
        String bodyInput = "resources/projects/projectWithIcon/mse/mse-codebase.xml";

        response = S2IOFacade.put( new FileRequestEntity( new File( bodyInput ) ), putURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/mse-codebase-icon.png
        // 201 Created

        String iconPutURL = projectBase + "mse-codebase-icon.png";
        String iconBodyInput = "resources/projects/projectWithIcon/mse/mse-codebase-icon.png";

        response =
            S2IOFacade.put( new FileRequestEntity( new File( iconBodyInput ) ), iconPutURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        File file = new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + PROJECT_DESCRIPTOR_FILENAME );
        assertTrue( file.exists() );
        assertEquals( readstream( new FileInputStream( file ) ), readstream( new FileInputStream( bodyInput ) ) );

        File iconFile = new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + PROJECT_ICON_FILENAME );
        assertTrue( iconFile.exists() );
        assertEquals( readstream( new FileInputStream( iconFile ) ), readstream( new FileInputStream( iconBodyInput ) ) );
    }

    public void testPublishProjectWithIcon_Authenticated_ImplicitCredentials()
        throws Exception
    {
        newHttpServer().start();
        String serverBase = server.getHttpUrl() + "/catalog/secured";
        AuthFacade.getAuthService().save( serverBase, VALID_USERNAME, PASSWORD );

        // HEAD /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/
        // 404 Not Found

        String projectBase =
            server.getHttpUrl() + "/catalog/secured/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/";
        ServerResponse response = S2IOFacade.head( projectBase, timeout_ms, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/mse-codebase.xml
        // 201 Created

        String putURL = projectBase + "mse-codebase.xml";
        String bodyInput = "resources/projects/projectWithIcon/mse/mse-codebase.xml";

        response = S2IOFacade.put( new FileRequestEntity( new File( bodyInput ) ), putURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/mse-codebase-icon.png
        // 201 Created

        String iconPutURL = projectBase + "mse-codebase-icon.png";
        String iconBodyInput = "resources/projects/projectWithIcon/mse/mse-codebase-icon.png";

        response =
            S2IOFacade.put( new FileRequestEntity( new File( iconBodyInput ) ), iconPutURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        File file = new File( getTmpDir( "secured" ), "test/projectWithIcon/1.0.0/" + PROJECT_DESCRIPTOR_FILENAME );
        assertTrue( file.exists() );
        assertEquals( readstream( new FileInputStream( file ) ), readstream( new FileInputStream( bodyInput ) ) );

        File iconFile = new File( getTmpDir( "secured" ), "test/projectWithIcon/1.0.0/" + PROJECT_ICON_FILENAME );
        assertTrue( iconFile.exists() );
        assertEquals( readstream( new FileInputStream( iconFile ) ), readstream( new FileInputStream( iconBodyInput ) ) );
    }

    public void testPublishProjectWithPreferences()
        throws Exception
    {
        newHttpServer().start();

        // HEAD /catalog/content/repositories/nx-codebase-repo/test/projectWithPreferences/1.0.0/
        // 404 Not Found

        String projectBase =
            server.getHttpUrl() + "/catalog/content/repositories/nx-codebase-repo/test/projectWithPreferences/1.0.0/";
        ServerResponse response = S2IOFacade.head( projectBase, timeout_ms, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode() );

        // PUT
        // /catalog/content/repositories/nx-codebase-repo/test/projectWithPreferences/1.0.0/mse-codebase-preferences.jar
        // 201 Created

        String prefPutURL = projectBase + "mse-codebase-preferences.jar";
        String prefBodyInput = "resources/projects/projectWithPreferences/mse/mse-codebase-preferences.jar";

        response =
            S2IOFacade.put( new FileRequestEntity( new File( prefBodyInput ) ), prefPutURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithPreferences/1.0.0/mse-codebase.xml
        // 201 Created

        String putURL = projectBase + "mse-codebase.xml";
        String bodyInput = "resources/projects/projectWithPreferences/mse/mse-codebase.xml";

        response = S2IOFacade.put( new FileRequestEntity( new File( bodyInput ) ), putURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        File file = new File( getTmpDir( "" ), "test/projectWithPreferences/1.0.0/" + PROJECT_DESCRIPTOR_FILENAME );
        assertTrue( file.exists() );
        assertEquals( readstream( new FileInputStream( file ) ), readstream( new FileInputStream( bodyInput ) ) );

        File prefFile = new File( getTmpDir( "" ), "test/projectWithPreferences/1.0.0/" + PROJECT_PREFERENCES_FILENAME );
        assertTrue( prefFile.exists() );
        assertEquals( readstream( new FileInputStream( prefFile ) ), readstream( new FileInputStream( prefBodyInput ) ) );
    }

    public void testPublishTwoProjects()
        throws Exception
    {
        newHttpServer().start();

        /*
         * FIRST ONE
         */

        // HEAD /catalog/content/repositories/nx-codebase-repo/test/projectWithoutIcon/1.0.0/
        // 404 Not Found

        String projectBase =
            server.getHttpUrl() + "/catalog/content/repositories/nx-codebase-repo/test/projectWithoutIcon/1.0.0/";
        ServerResponse response = S2IOFacade.head( projectBase, timeout_ms, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithoutIcon/1.0.0/mse-codebase.xml
        // 201 Created

        String putURL = projectBase + "mse-codebase.xml";
        String bodyInput = "resources/projects/projectWithoutIcon/mse/mse-codebase.xml";

        response = S2IOFacade.put( new FileRequestEntity( new File( bodyInput ) ), putURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        /*
         * SECOND ONE
         */

        // HEAD /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/
        // 404 Not Found

        String projectBase2 =
            server.getHttpUrl() + "/catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/";
        response = S2IOFacade.head( projectBase2, timeout_ms, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/mse-codebase.xml
        // 201 Created

        putURL = projectBase2 + "mse-codebase.xml";
        String bodyInput2 = "resources/projects/projectWithIcon/mse/mse-codebase.xml";

        response = S2IOFacade.put( new FileRequestEntity( new File( bodyInput2 ) ), putURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        // PUT /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/mse-codebase-icon.png
        // 201 Created

        String iconPutURL = projectBase2 + "mse-codebase-icon.png";
        String iconBodyInput = "resources/projects/projectWithIcon/mse/mse-codebase-icon.png";

        response =
            S2IOFacade.put( new FileRequestEntity( new File( iconBodyInput ) ), iconPutURL, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_CREATED, response.getStatusCode() );

        /*
         * Check contents
         */

        File file = new File( getTmpDir( "" ), "test/projectWithoutIcon/1.0.0/" + PROJECT_DESCRIPTOR_FILENAME );
        assertTrue( file.exists() );
        assertEquals( readstream( new FileInputStream( file ) ), readstream( new FileInputStream( bodyInput ) ) );
        assertEquals( 1, new File( getTmpDir( "" ), "test/projectWithoutIcon/1.0.0/" ).list().length );

        File file2 = new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + PROJECT_DESCRIPTOR_FILENAME );
        assertTrue( file2.exists() );
        assertEquals( readstream( new FileInputStream( file2 ) ), readstream( new FileInputStream( bodyInput2 ) ) );

        File iconFile = new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" + PROJECT_ICON_FILENAME );
        assertTrue( iconFile.exists() );
        assertEquals( readstream( new FileInputStream( iconFile ) ), readstream( new FileInputStream( iconBodyInput ) ) );
    }

    public void testPublishExisting()
        throws Exception
    {
        newHttpServer().start();

        try
        {
            File tmpRep = new File( getTmpDir( "" ), "test/projectWithIcon/1.0.0/" );
            tmpRep.mkdirs();
            copyDirectory( new File( "resources/projects/projectWithIcon" ), tmpRep );
        }
        catch ( Exception e )
        {
            fail( "Failed to copy test" );
        }
        // HEAD /catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/
        // 200 OK

        String projectBase =
            server.getHttpUrl() + "/catalog/content/repositories/nx-codebase-repo/test/projectWithIcon/1.0.0/";
        ServerResponse response = S2IOFacade.head( projectBase, timeout_ms, new NullProgressMonitor() );
        assertEquals( HttpURLConnection.HTTP_OK, response.getStatusCode() );

    }

}
