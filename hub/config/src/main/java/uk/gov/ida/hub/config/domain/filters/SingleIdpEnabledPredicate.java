package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import java.util.function.Predicate;

public class SingleIdpEnabledPredicate implements Predicate<IdentityProviderConfig> {

    @Override
    public boolean test(IdentityProviderConfig identityProviderConfig) {
        return identityProviderConfig.isEnabledForSingleIdp();
    }
}
