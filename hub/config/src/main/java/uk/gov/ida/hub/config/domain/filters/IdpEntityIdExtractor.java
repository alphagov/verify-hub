package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Function;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

public class IdpEntityIdExtractor implements Function<IdentityProviderConfigEntityData, String> {

    @Override
    public String apply(IdentityProviderConfigEntityData input) {
        return input.getEntityId();
    }
}
