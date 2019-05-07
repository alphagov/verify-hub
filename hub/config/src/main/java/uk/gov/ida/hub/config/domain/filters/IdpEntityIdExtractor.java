package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Function;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

public class IdpEntityIdExtractor implements Function<IdentityProviderConfig, String> {

    @Override
    public String apply(IdentityProviderConfig input) {
        return input.getEntityId();
    }
}
