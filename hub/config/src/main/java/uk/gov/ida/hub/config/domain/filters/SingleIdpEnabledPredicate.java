package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

public class SingleIdpEnabledPredicate implements Predicate<IdentityProviderConfig> {

    @Override
    public boolean apply(IdentityProviderConfig identityProviderConfig) {
        return identityProviderConfig.isEnabledForSingleIdp();
    }
}
