package uk.gov.ida.hub.config.domain.filters;

import org.joda.time.DateTime;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;


public class NewUserIdpPredicateTest {
    @Test
    public void apply_shouldReturnTrue_whenIdpRegistrationIsEnabled() {
        Predicate<IdentityProviderConfig> newUserIdpPredicate = new NewUserIdpPredicate();

        IdentityProviderConfig disconnectingIdp = anIdentityProviderConfigData()
                .withProvideRegistrationUntil(DateTime.now().plusDays(1))
                .build();

        assertThat(newUserIdpPredicate.test(disconnectingIdp)).isTrue();
    }

    @Test
    public void apply_shouldReturnFalse_whenIdpRegistrationIsDisabled() {
        Predicate<IdentityProviderConfig> newUserIdpPredicate = new NewUserIdpPredicate();

        IdentityProviderConfig disconnectingIdp = anIdentityProviderConfigData()
                .withProvideRegistrationUntil(DateTime.now().minusDays(1))
                .build();

        assertThat(newUserIdpPredicate.test(disconnectingIdp)).isFalse();
    }
}
