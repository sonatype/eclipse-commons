package org.maven.ide.eclipse.authentication.internal.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PasswordProviderDelegate
{
    private final static Logger log = LoggerFactory.getLogger( PasswordProviderDelegate.class );

    protected int retryCount;

    public boolean retryOnError( Exception e, IPreferencesContainer container )
    {
        return ++retryCount <= 3;
    }

    public abstract PBEKeySpec getPassword( IPreferencesContainer container, int passwordType );

    protected static PBEKeySpec newPassword( String password )
    {
        if ( password == null )
        {
            return null;
        }

        // shameless copy from org.eclipse.equinox.internal.security.ui.storage.StorageLoginDialog.okPressed()

        String internalPassword;
        try
        {
            // normally use digest of what was entered
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            byte[] digested = digest.digest( password.getBytes() );
            internalPassword = EncodingUtils.encodeBase64( digested );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // just use the text as is
            log.info( "No digest algorithm", e );
            internalPassword = password;
        }
        return new PBEKeySpec( internalPassword.toCharArray() );
    }

}
