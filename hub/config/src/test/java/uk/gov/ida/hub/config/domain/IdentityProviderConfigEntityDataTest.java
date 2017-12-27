package uk.gov.ida.hub.config.domain;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class IdentityProviderConfigEntityDataTest {

    private final IdentityProviderConfigDataBuilder dataBuilder = anIdentityProviderConfigData();

    @Test
    public void should_defaultSupportedLevelsOfAssuranceToOnlyIncludeLOA2() {
        IdentityProviderConfigEntityData data = dataBuilder.build();

        assertThat(data.getSupportedLevelsOfAssurance()).containsExactly(LevelOfAssurance.LEVEL_2);
    }

    @Test
    public void shouldReturnAllOnboardingEntityIds() {
        IdentityProviderConfigEntityData data = dataBuilder
                .withOnboarding(Arrays.asList("EID1", "EID2"))
                .withOnboardingTemp(Arrays.asList("EID_OT1", "EID_OT2"))
                .build();

        final List<String> onboardingEntityIds = data.getOnboardingTransactionEntityIds();
        assertThat(onboardingEntityIds.size()).isEqualTo(2);
        assertThat(onboardingEntityIds).contains("EID1");
        assertThat(onboardingEntityIds).contains("EID2");

        final List<String> onboardingEntityIdsTemp = data.getOnboardingTransactionEntityIdsTemp();
        assertThat(onboardingEntityIdsTemp.size()).isEqualTo(4);
        assertThat(onboardingEntityIdsTemp).contains("EID1");
        assertThat(onboardingEntityIdsTemp).contains("EID2");
        assertThat(onboardingEntityIdsTemp).contains("EID_OT1");
        assertThat(onboardingEntityIdsTemp).contains("EID_OT2");
    }

    @Test
    public void shouldCheckThatIsOnboardingForTransactionEntity() {
        final IdentityProviderConfigEntityData dataWithoutOnboarding = dataBuilder.withEntityId("EID").build();
        assertThat(dataWithoutOnboarding.isOnboardingForTransactionEntity("EID")).isFalse();

        final IdentityProviderConfigEntityData data = dataBuilder
                .withOnboarding(Collections.singletonList("EID_O"))
                .withOnboardingTemp(Collections.singletonList("EID_OT"))
                .build();

        assertThat(data.isOnboardingForTransactionEntity("EID_O")).isTrue();
        assertThat(data.isOnboardingForTransactionEntity("EID_OT")).isTrue();
    }

    @Test
    public void shouldCheckThatIsOnboardingAtAllLevels() {
        final IdentityProviderConfigEntityData dataOnboardingAtAllLevels = dataBuilder
                .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .build();

        assertThat(dataOnboardingAtAllLevels.isOnboardingAtAllLevels()).isTrue();

        final IdentityProviderConfigEntityData dataOnboardingAtOneLevelOnly = dataBuilder
                .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
                .build();

        assertThat(dataOnboardingAtOneLevelOnly.isOnboardingAtAllLevels()).isFalse();
    }

    @Test
    public void shouldCheckThatIsOnboardingAtLoa() {
        final IdentityProviderConfigEntityData data = dataBuilder
                .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
                .build();

        assertThat(data.isOnboardingAtLoa(LevelOfAssurance.LEVEL_1)).isTrue();
        assertThat(data.isOnboardingAtLoa(LevelOfAssurance.LEVEL_2)).isFalse();
    }

}
