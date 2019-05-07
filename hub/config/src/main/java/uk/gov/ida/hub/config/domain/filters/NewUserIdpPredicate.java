package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import javax.annotation.Nullable;

public class NewUserIdpPredicate implements Predicate<IdentityProviderConfig> {
	@Override
	public boolean apply(@Nullable IdentityProviderConfig identityProviderConfig) {
		return identityProviderConfig.isRegistrationEnabled();
	}
}
