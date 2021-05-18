package uk.gov.ida.hub.policy.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LevelOfAssuranceTest {

    @Test
    public void testLevelOrdinalsHaveNotBeenChanged() {
        assertThat(LevelOfAssurance.LEVEL_X.ordinal()).isEqualTo(0);
        assertThat(LevelOfAssurance.LEVEL_1.ordinal()).isEqualTo(1);
        assertThat(LevelOfAssurance.LEVEL_2.ordinal()).isEqualTo(2);
        assertThat(LevelOfAssurance.LEVEL_3.ordinal()).isEqualTo(3);
        assertThat(LevelOfAssurance.LEVEL_4.ordinal()).isEqualTo(4);
    }

}