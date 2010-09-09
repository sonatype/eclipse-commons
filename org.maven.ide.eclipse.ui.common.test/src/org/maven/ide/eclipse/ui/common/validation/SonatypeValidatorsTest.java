package org.maven.ide.eclipse.ui.common.validation;

import junit.framework.TestCase;

import org.netbeans.validation.api.Problems;

public class SonatypeValidatorsTest
    extends TestCase
{
    public void testValidHttpUrl()
    {
        Problems p = new Problems();
        SonatypeValidators.createRemoteHttpUrlValidators().validate( p, "testfield", "http://devclone-55.1515.mtvi.com:8081/nexus" );
        assertFalse( p.toString(), p.hasFatal() );
    }

    public void testInvalidHttpUrl()
    {
        String url = "http://localhost:8081/nexus/foo%";
        Problems p = new Problems();
        SonatypeValidators.createRemoteHttpUrlValidators().validate( p, "testfield", url );
        assertTrue( p.toString(), p.hasFatal() );

        assertTrue( p.getLeadProblem().getMessage(),
                    p.getLeadProblem().getMessage().startsWith( "'" + url + "' is not a valid URL" ) );
    }

    public void testValidScmUrl()
    {
        Problems p = new Problems();
        SonatypeValidators.URL_MUST_BE_VALID.validate( p, "testfield", "scm:git:github.com:sonatype/sonatype-tycho.git" );
        assertFalse( p.toString(), p.hasFatal() );
    }

    public void testInvalidScmUrl()
    {
        Problems p = new Problems();
        SonatypeValidators.URL_MUST_BE_VALID.validate( p, "testfield", "scm:x" );
        assertTrue( p.toString(), p.hasFatal() );
        assertEquals( "'scm:x' is not a valid URL: SCM URI 'scm:x' does not specify SCM type",
                      p.getLeadProblem().getMessage() );
    }
}
