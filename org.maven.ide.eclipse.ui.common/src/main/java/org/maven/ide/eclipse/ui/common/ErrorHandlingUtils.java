package org.maven.ide.eclipse.ui.common;

import java.io.IOException;
import java.nio.channels.UnresolvedAddressException;

import org.maven.ide.eclipse.io.ForbiddenException;
import org.maven.ide.eclipse.io.NotFoundException;
import org.maven.ide.eclipse.io.TransferException;
import org.maven.ide.eclipse.io.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandlingUtils
{
    private static Logger log = LoggerFactory.getLogger( ErrorHandlingUtils.class );

    private ErrorHandlingUtils()
    {
    }

    /**
     * Handle the different types of exceptions we have for various error responses in io plugin. If we introduce new
     * exception type, we shall add a new parameter to this method and "It's a good thing". if you intentionally choose
     * not to be interested in one or more exceptions just pass null in it's parameter Currently supported exception
     * types: ForbiddenException, NotFoundException, UnauthorizedException and TransferException
     * 
     * @param exc exception passed in, null allowed
     * @return an error string or null if no matching error found
     */
    public static String convertNexusIOExceptionToUIText( Throwable exc, String auth, String forbidden, String notFound )
    {
        if ( exc == null )
        {
            return null;
        }
        if ( auth == null )
        {
            auth = Messages.errors_authFailed;
        }
        if ( forbidden == null )
        {
            forbidden = Messages.errors_forbidden;
        }
        if ( notFound == null )
        {
            notFound = Messages.errors_resourceNotFound;
        }
        
        log.debug( exc.getMessage(), exc );
        
        if ( exc instanceof ForbiddenException )
        {
            return forbidden;
        }
        if ( exc instanceof NotFoundException )
        {
            return notFound;
        }
        if ( exc instanceof UnauthorizedException )
        {
            return auth;
        }
        if ( exc instanceof TransferException )
        {
            TransferException te = (TransferException) exc;
            if ( te.hasNexusError() )
            {
                return te.getNexusError();
            }
        }
        if ( exc instanceof IOException && exc.getCause() instanceof UnresolvedAddressException )
        {
            return Messages.errors_unresolvedAddress;
        }
        if ( exc != null && exc.getCause() != null && exc != exc.getCause() )
        {
            return convertNexusIOExceptionToUIText( exc.getCause(), auth, forbidden, notFound );
        }
        return null;
    }

    public static String convertNexusIOExceptionToUIText( Throwable e )
    {
        return ErrorHandlingUtils.convertNexusIOExceptionToUIText( e, Messages.errors_authFailed,
                                                                   Messages.errors_forbidden,
                                                                   Messages.errors_resourceNotFound );
    }
}
