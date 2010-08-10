package org.maven.ide.eclipse.ui.common.test;

import junit.framework.TestCase;

import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.Problems;

public class SonatypeValidatorsTest
    extends TestCase
{


    public void testMECLIPSE1317()
    {
        Problems p = new Problems();
        SonatypeValidators.createRemoteHttpUrlValidators().validate( p, "testfield", "http://devclone-55.1515.mtvi.com:8081/nexus" );
        assertFalse( p.hasFatal() );
        
        p = new Problems();
        SonatypeValidators.createRemoteHttpUrlValidators().validate( p, "testfield", "http://localhost:8081/nexus/foo%" );
        assertTrue( p.hasFatal() );
    }


}
