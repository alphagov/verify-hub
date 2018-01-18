package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Collection;

import static com.google.common.base.Optional.fromNullable;

public class SuccessfulMatchStateController extends AbstractSuccessfulMatchStateController {
    public SuccessfulMatchStateController(AbstractSuccessfulMatchState state, ResponseFromHubFactory responseFromHubFactory, IdentityProvidersConfigProxy identityProvidersConfigProxy) {
        super(state, responseFromHubFactory, identityProvidersConfigProxy);
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
        Collection<String> enabledIdentityProviders = identityProvidersConfigProxy.getEnabledIdentityProviders(
                fromNullable(state.getRequestIssuerEntityId()));

        if (!enabledIdentityProviders.contains(state.getIdentityProviderEntityId())) {
            throw new IdpDisabledException(state.getIdentityProviderEntityId());
        }

        return getResponse();
    }
}
