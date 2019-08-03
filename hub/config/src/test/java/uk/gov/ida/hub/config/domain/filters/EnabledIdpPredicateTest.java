package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class EnabledIdpPredicateTest {

    @Test
    public void shouldBeTrueForEnabled() throws Exception {
        Predicate<IdentityProviderConfig> enabledPredicate = new EnabledIdpPredicate();

        IdentityProviderConfig enabledIdp = anIdentityProviderConfigData()
                .withEnabled(true)
                .build();

        assertThat(enabledPredicate.test(enabledIdp)).isTrue();
    }

    @Test
    public void shouldBeFalseForDisabled() throws Exception {
        Predicate<IdentityProviderConfig> enabledPredicate = new EnabledIdpPredicate();

        IdentityProviderConfig disabledIdp = anIdentityProviderConfigData()
                .withEnabled(false)
                .build();

        assertThat(enabledPredicate.test(disabledIdp)).isFalse();
    }
}
