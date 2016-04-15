package com.pason.plugins.artifactorypolling;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtifactoryUtilsTest {

    @Test
    public void testIsDynamic(){
        assertTrue(ArtifactoryUtils.isVersionDynamic("3.20.2.+"));
        assertTrue(!ArtifactoryUtils.isVersionDynamic("3.20.2.0"));
        assertTrue(ArtifactoryUtils.isVersionDynamic("+"));
    }   
}