package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

public class SingleIdpEnabledPredicate implements Predicate<IdentityProviderConfigEntityData> {

    @Override
    public boolean apply(IdentityProviderConfigEntityData identityProviderConfigEntityData) {
        return identityProviderConfigEntityData.isEnabledForSingleIdp();
    }
}
