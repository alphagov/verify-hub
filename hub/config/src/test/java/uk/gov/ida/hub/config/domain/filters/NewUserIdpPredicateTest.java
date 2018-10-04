package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import org.joda.time.DateTime;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;


public class NewUserIdpPredicateTest {
	@Test
	public void apply_shouldReturnTrue_whenIdpRegistrationIsEnabled() {
		Predicate<IdentityProviderConfigEntityData> newUserIdpPredicate = new NewUserIdpPredicate();
		
		IdentityProviderConfigEntityData disconnectingIdp = anIdentityProviderConfigData()
				.withProvideRegistrationUntil(DateTime.now().plusDays(1))
				.build();
		
		assertThat(newUserIdpPredicate.apply(disconnectingIdp)).isTrue();
	}
	
	@Test
	public void apply_shouldReturnFalse_whenIdpRegistrationIsDisabled() {
		Predicate<IdentityProviderConfigEntityData> newUserIdpPredicate = new NewUserIdpPredicate();
		
		IdentityProviderConfigEntityData disconnectingIdp = anIdentityProviderConfigData()
				.withProvideRegistrationUntil(DateTime.now().minusDays(1))
				.build();
		
		assertThat(newUserIdpPredicate.apply(disconnectingIdp)).isFalse();
	}
}
