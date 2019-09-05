package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import java.util.function.Predicate;
import javax.annotation.Nullable;

public class NewUserIdpPredicate implements Predicate<IdentityProviderConfig> {
	@Override
	public boolean test(@Nullable IdentityProviderConfig identityProviderConfig) {
		return identityProviderConfig.isRegistrationEnabled();
	}
}
