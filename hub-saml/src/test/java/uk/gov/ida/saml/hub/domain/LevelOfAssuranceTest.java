package uk.gov.ida.saml.hub.domain;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LevelOfAssuranceTest {

    @Test
    public void checkOrdering() {
        assertTrue(LevelOfAssurance.HIGH.compareTo(LevelOfAssurance.SUBSTANTIAL) > 0);
        assertTrue(LevelOfAssurance.HIGH.compareTo(LevelOfAssurance.LOW) > 0);
        assertTrue(LevelOfAssurance.SUBSTANTIAL.compareTo(LevelOfAssurance.LOW) > 0);

        assertTrue(LevelOfAssurance.SUBSTANTIAL.compareTo(LevelOfAssurance.HIGH) < 0);
        assertTrue(LevelOfAssurance.LOW.compareTo(LevelOfAssurance.HIGH) < 0);
        assertTrue(LevelOfAssurance.LOW.compareTo(LevelOfAssurance.SUBSTANTIAL) < 0);
    }

}