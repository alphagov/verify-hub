package uk.gov.ida.saml.hub.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LevelOfAssuranceTest {

    @Test
    public void checkOrdering() {
        assertThat(LevelOfAssurance.HIGH.compareTo(LevelOfAssurance.SUBSTANTIAL)).isGreaterThan(0);
        assertThat(LevelOfAssurance.HIGH.compareTo(LevelOfAssurance.LOW)).isGreaterThan(0);
        assertThat(LevelOfAssurance.SUBSTANTIAL.compareTo(LevelOfAssurance.LOW)).isGreaterThan(0);

        assertThat(LevelOfAssurance.SUBSTANTIAL.compareTo(LevelOfAssurance.HIGH)).isLessThan(0);
        assertThat(LevelOfAssurance.LOW.compareTo(LevelOfAssurance.HIGH)).isLessThan(0);
        assertThat(LevelOfAssurance.LOW.compareTo(LevelOfAssurance.SUBSTANTIAL)).isLessThan(0);
    }
}
