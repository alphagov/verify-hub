package uk.gov.ida.hub.config.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class IdentityProviderConfigTest {

    private final IdentityProviderConfigDataBuilder dataBuilder = anIdentityProviderConfigData();

    private final DateTime expiredDatetime = DateTime.now().minusDays(1);
    private final DateTime futureDatetime = DateTime.now().plusDays(1);
    private final Duration sessionDuration = new Duration(90 * 60 * 1000);

    @Test
    public void should_defaultSupportedLevelsOfAssuranceToOnlyIncludeLOA2() {
        IdentityProviderConfig data = dataBuilder.build();

        assertThat(data.getSupportedLevelsOfAssurance()).containsExactly(LevelOfAssurance.LEVEL_2);
    }

    @Test
    public void shouldReturnTrueForRegistrationEnabled_whenProvideRegistrationUntilHasNotBeenSpecified() {
        IdentityProviderConfig data = dataBuilder.build();
        data.provideRegistrationUntil = "";

        assertThat(data.canReceiveRegistrationRequests()).isEqualTo(true);
        assertThat(data.canSendRegistrationResponses(sessionDuration)).isEqualTo(true);
    }

    @Test
    public void shouldReturnTrueForRegistrationEnabled_whenProvideRegistrationUntilDateHasNotExpired() {
        IdentityProviderConfig data = dataBuilder
                .withProvideRegistrationUntil(futureDatetime)
                .build();

        assertThat(data.canReceiveRegistrationRequests()).isEqualTo(true);
        assertThat(data.canSendRegistrationResponses(sessionDuration)).isEqualTo(true);
    }

    @Test
    public void shouldReturnFalseForRegistrationEnabled_whenProvideRegistrationUntilDateHasExpired() {
        IdentityProviderConfig data = dataBuilder
                .withProvideRegistrationUntil(expiredDatetime)
                .build();

        assertThat(data.canReceiveRegistrationRequests()).isEqualTo(false);
        assertThat(data.canSendRegistrationResponses(sessionDuration)).isEqualTo(false);
    }

    @Test
    public void shouldReturnTrueForDisconnectingForRegistration_whenResponseWithinSessionDurationFromCutOffTime() {
        IdentityProviderConfig data = dataBuilder
                .withProvideRegistrationUntil(DateTime.now().minusMinutes((int) sessionDuration.getStandardMinutes()).plusSeconds(10))
                .build();

        assertThat(data.canReceiveRegistrationRequests()).isEqualTo(false);
        assertThat(data.canSendRegistrationResponses(sessionDuration)).isEqualTo(true);
    }

    @Test
    public void shouldThrowInvalidFormatException_whenProvideRegistrationUntilHasBeenSpecifiedButIsInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IdentityProviderConfig data = dataBuilder.build();
            data.provideRegistrationUntil = "2020-09-09";
            data.canReceiveRegistrationRequests();
        });
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

    @Test
    public void shouldThrowInvalidFormatException_whenProvideAuthenticationUntilHasBeenSpecifiedButIsInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IdentityProviderConfig data = dataBuilder.build();
            data.provideAuthenticationUntil = "2020-09-09";
            data.isAuthenticationEnabled();
        });
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
