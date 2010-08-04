package org.maven.ide.eclipse.ui.common.test;

import junit.framework.TestCase;

import org.maven.ide.eclipse.io.ForbiddenException;
import org.maven.ide.eclipse.io.NotFoundException;
import org.maven.ide.eclipse.io.UnauthorizedException;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;

public class ErrorHandlingUtilsTest
    extends TestCase
{
    private static final String FORBIDDEN = "forbidden";

    private static final String AUTH = "auth";

    private static final String NOT_FOUND = "notFound";

    public void testForbidden()
    {
        assertTrue( FORBIDDEN.equals( getErrorMessage( new ForbiddenException( "Forbidden" ) ) ) );
    }

    public void testNotFound()
    {
        assertTrue( NOT_FOUND.equals( getErrorMessage( new NotFoundException( "NotFound" ) ) ) );
    }

    public void testNotAuthorized()
    {
        assertTrue( AUTH.equals( getErrorMessage( new UnauthorizedException( "Forbidden" ) ) ) );
    }

    public void testRecursive()
    {
        assertTrue( AUTH.equals( getErrorMessage( new Exception( "Message", new UnauthorizedException( "Unauthorized" ) ) ) ) );
    }

    public void testUnknown()
    {
        assertNull( getErrorMessage( new Exception( "Message" ) ) );
    }

    private String getErrorMessage( Exception exc )
    {
        return ErrorHandlingUtils.convertNexusIOExceptionToUIText( exc, AUTH, FORBIDDEN, NOT_FOUND );
    }

}
