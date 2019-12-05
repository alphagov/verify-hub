package uk.gov.ida.saml.hub.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LevelOfAssuranceTest {

    @Test
    public void checkOrdering() {
        assertThat(LevelOfAssurance.HIGH.compareTo(LevelOfAssurance.SUBSTANTIAL) > 0);
        assertThat(LevelOfAssurance.HIGH.compareTo(LevelOfAssurance.LOW) > 0);
        assertThat(LevelOfAssurance.SUBSTANTIAL.compareTo(LevelOfAssurance.LOW) > 0);

        assertThat(LevelOfAssurance.SUBSTANTIAL.compareTo(LevelOfAssurance.HIGH) < 0);
        assertThat(LevelOfAssurance.LOW.compareTo(LevelOfAssurance.HIGH) < 0);
        assertThat(LevelOfAssurance.LOW.compareTo(LevelOfAssurance.SUBSTANTIAL) < 0);
    }
}
