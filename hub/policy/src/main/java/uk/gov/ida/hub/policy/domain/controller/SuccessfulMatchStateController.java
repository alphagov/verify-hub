package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Collection;

public class SuccessfulMatchStateController extends AbstractSuccessfulMatchStateController<SuccessfulMatchState> {

    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    public SuccessfulMatchStateController(
            final SuccessfulMatchState state,
            final ResponseFromHubFactory responseFromHubFactory,
            final IdentityProvidersConfigProxy identityProvidersConfigProxy) {

        super(state, responseFromHubFactory);

        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
        Collection<String> enabledIdentityProviders = identityProvidersConfigProxy.getEnabledIdentityProviders(
                state.getRequestIssuerEntityId(), state.isRegistering(), state.getLevelOfAssurance());

        if (!enabledIdentityProviders.contains(state.getIdentityProviderEntityId())) {
            throw new IdpDisabledException(state.getIdentityProviderEntityId());
        }

        return getResponse();
    }
}
