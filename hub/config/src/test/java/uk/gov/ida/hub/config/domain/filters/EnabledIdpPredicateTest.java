package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class EnabledIdpPredicateTest {

    @Test
    public void shouldBeTrueForEnabled() throws Exception {
        Predicate<IdentityProviderConfigEntityData> enabledPredicate = new EnabledIdpPredicate();

        IdentityProviderConfigEntityData enabledIdp = anIdentityProviderConfigData()
                .withEnabled(true)
                .build();

        assertThat(enabledPredicate.apply(enabledIdp)).isTrue();
    }

    @Test
    public void shouldBeFalseForDisabled() throws Exception {
        Predicate<IdentityProviderConfigEntityData> enabledPredicate = new EnabledIdpPredicate();

        IdentityProviderConfigEntityData disabledIdp = anIdentityProviderConfigData()
                .withEnabled(false)
                .build();

        assertThat(enabledPredicate.apply(disabledIdp)).isFalse();
    }
}
