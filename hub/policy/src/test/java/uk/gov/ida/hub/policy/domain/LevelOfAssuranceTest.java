package uk.gov.ida.hub.policy.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    @Test
    public void testLevelOfAssuranceComparisons() {
        assertThat(LevelOfAssurance.LEVEL_1.lessThan(LevelOfAssurance.LEVEL_2)).isTrue();
        assertThat(LevelOfAssurance.LEVEL_2.greaterThan(LevelOfAssurance.LEVEL_1)).isTrue();

        assertThat(LevelOfAssurance.LEVEL_2.lessThan(LevelOfAssurance.LEVEL_3)).isTrue();
        assertThat(LevelOfAssurance.LEVEL_3.greaterThan(LevelOfAssurance.LEVEL_2)).isTrue();

        assertThat(LevelOfAssurance.LEVEL_1.lessThan(LevelOfAssurance.LEVEL_1)).isFalse();
        assertThat(LevelOfAssurance.LEVEL_1.greaterThan(LevelOfAssurance.LEVEL_1)).isFalse();
    }

    @Test
    public void testLevelOfAssuranceMinMax() {
        List<LevelOfAssurance> assurances;

        assurances = Arrays.asList(
            LevelOfAssurance.LEVEL_1,
            LevelOfAssurance.LEVEL_2);

        assertThat(LevelOfAssurance.min(assurances)).isEqualTo(LevelOfAssurance.LEVEL_1);
        assertThat(LevelOfAssurance.max(assurances)).isEqualTo(LevelOfAssurance.LEVEL_2);

        assurances = Arrays.asList(
            LevelOfAssurance.LEVEL_2,
            LevelOfAssurance.LEVEL_1);

        assertThat(LevelOfAssurance.min(assurances)).isEqualTo(LevelOfAssurance.LEVEL_1);
        assertThat(LevelOfAssurance.max(assurances)).isEqualTo(LevelOfAssurance.LEVEL_2);

        assurances = Arrays.asList(
            LevelOfAssurance.LEVEL_1,
            LevelOfAssurance.LEVEL_1,
            LevelOfAssurance.LEVEL_1,
            LevelOfAssurance.LEVEL_2);

        assertThat(LevelOfAssurance.min(assurances)).isEqualTo(LevelOfAssurance.LEVEL_1);
        assertThat(LevelOfAssurance.max(assurances)).isEqualTo(LevelOfAssurance.LEVEL_2);

        assurances = Arrays.asList(
            LevelOfAssurance.LEVEL_3,
            LevelOfAssurance.LEVEL_1,
            LevelOfAssurance.LEVEL_2);

        assertThat(LevelOfAssurance.min(assurances)).isEqualTo(LevelOfAssurance.LEVEL_1);
        assertThat(LevelOfAssurance.max(assurances)).isEqualTo(LevelOfAssurance.LEVEL_3);

        assurances = Collections.singletonList(LevelOfAssurance.LEVEL_1);

        assertThat(LevelOfAssurance.min(assurances)).isEqualTo(LevelOfAssurance.LEVEL_1);
        assertThat(LevelOfAssurance.max(assurances)).isEqualTo(LevelOfAssurance.LEVEL_1);

        assurances = new ArrayList<>();

        assertThat(LevelOfAssurance.min(assurances)).isNull();
        assertThat(LevelOfAssurance.max(assurances)).isNull();

        assurances = null;

        assertThat(LevelOfAssurance.min(assurances)).isNull();
        assertThat(LevelOfAssurance.max(assurances)).isNull();
    }

}