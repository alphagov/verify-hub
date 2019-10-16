package uk.gov.ida.hub.config.domain;

import org.joda.time.DateTime;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class IdentityProviderConfigTest {

    private final IdentityProviderConfigDataBuilder dataBuilder = anIdentityProviderConfigData();

    private final DateTime expiredDatetime = DateTime.now().minusDays(1);
    private final DateTime futureDatetime = DateTime.now().plusDays(1);

    @Test
    public void should_defaultSupportedLevelsOfAssuranceToOnlyIncludeLOA2() {
        IdentityProviderConfig data = dataBuilder.build();

        assertThat(data.getSupportedLevelsOfAssurance()).containsExactly(LevelOfAssurance.LEVEL_2);
    }
    
    @Test
    public void shouldReturnTrueForRegistrationEnabled_whenProvideRegistrationUntilHasNotBeenSpecified() {
        IdentityProviderConfig data = dataBuilder.build();
        data.provideRegistrationUntil = "";

        assertThat(data.isRegistrationEnabled()).isEqualTo(true);
    }
    
    @Test
    public void shouldReturnTrueForRegistrationEnabled_whenProvideRegistrationUntilDateHasNotExpired() {
        IdentityProviderConfig data = dataBuilder
            .withProvideRegistrationUntil(futureDatetime)
            .build();
        
        assertThat(data.isRegistrationEnabled()).isEqualTo(true);
    }
    
    @Test
    public void shouldReturnFalseForRegistrationEnabled_whenProvideRegistrationUntilDateHasExpired() {
        IdentityProviderConfig data = dataBuilder
            .withProvideRegistrationUntil(expiredDatetime)
            .build();
        
        assertThat(data.isRegistrationEnabled()).isEqualTo(false);
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void shouldThrowInvalidFormatException_whenProvideRegistrationUntilHasBeenSpecifiedButIsInvalid() {
        IdentityProviderConfig data = dataBuilder.build();
        data.provideRegistrationUntil = "2020-09-09";
        data.isRegistrationEnabled();
    }

    @Test
    public void shouldReturnTrueForAuthenticationEnabled_whenProvideAuthenticationUntilHasNotBeenSpecified() {
        IdentityProviderConfig data = dataBuilder.build();
        data.provideAuthenticationUntil = "";

        assertThat(data.isAuthenticationEnabled()).isEqualTo(true);
    }
    
    @Test
    public void shouldReturnTrueForAuthenticationEnabled_whenProvideAuthenticationUntilDateHasNotExpired() {
        IdentityProviderConfig data = dataBuilder
            .withProvideAuthenticationUntil(futureDatetime)
            .build();
        
        assertThat(data.isAuthenticationEnabled()).isEqualTo(true);
    }
    
    @Test
    public void shouldReturnFalseForAuthenticationEnabled_whenProvideAuthenticationUntilDateHasExpired() {
        IdentityProviderConfig data = dataBuilder
            .withProvideAuthenticationUntil(expiredDatetime)
            .build();
        
        assertThat(data.isAuthenticationEnabled()).isEqualTo(false);
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void shouldThrowInvalidFormatException_whenProvideAuthenticationUntilHasBeenSpecifiedButIsInvalid() {
        IdentityProviderConfig data = dataBuilder.build();
        data.provideAuthenticationUntil = "2020-09-09";
        data.isAuthenticationEnabled();
    }

    @Test
    public void shouldCheckThatIsOnboardingForTransactionEntity() {
        final IdentityProviderConfig dataWithoutOnboarding = dataBuilder.withEntityId("EID").build();
        assertThat(dataWithoutOnboarding.isOnboardingForTransactionEntity("EID")).isFalse();

        final IdentityProviderConfig data = dataBuilder
                .withOnboarding(List.of("EID_O","EID_OT"))
                .build();

        assertThat(data.isOnboardingForTransactionEntity("EID_O")).isTrue();
        assertThat(data.isOnboardingForTransactionEntity("EID_OT")).isTrue();
    }

    @Test
    public void shouldCheckThatIsOnboardingAtAllLevels() {
        final IdentityProviderConfig dataOnboardingAtAllLevels = dataBuilder
                .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .build();

        assertThat(dataOnboardingAtAllLevels.isOnboardingAtAllLevels()).isTrue();

        final IdentityProviderConfig dataOnboardingAtOneLevelOnly = dataBuilder
                .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
                .build();

        assertThat(dataOnboardingAtOneLevelOnly.isOnboardingAtAllLevels()).isFalse();
    }

    @Test
    public void shouldCheckThatIsOnboardingAtLoa() {
        final IdentityProviderConfig data = dataBuilder
                .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
                .build();

        assertThat(data.isOnboardingAtLoa(LevelOfAssurance.LEVEL_1)).isTrue();
        assertThat(data.isOnboardingAtLoa(LevelOfAssurance.LEVEL_2)).isFalse();
    }

}
