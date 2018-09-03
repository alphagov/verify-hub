package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

import javax.annotation.Nullable;

public class NewUserIdpPredicate implements Predicate<IdentityProviderConfigEntityData> {
	@Override
	public boolean apply(@Nullable IdentityProviderConfigEntityData identityProviderConfigEntityData) {
		return identityProviderConfigEntityData.isRegistrationEnabled();
	}
}
