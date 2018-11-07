package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.NonMatchingRequestReceivedState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Collection;

public class NonMatchingRequestReceivedStateController implements ResponsePreparedStateController {

    private final NonMatchingRequestReceivedState state;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    public NonMatchingRequestReceivedStateController(
            final NonMatchingRequestReceivedState state,
            final ResponseFromHubFactory responseFromHubFactory,
            final IdentityProvidersConfigProxy identityProvidersConfigProxy) {
        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
        Collection<String> enabledIdentityProviders = identityProvidersConfigProxy.getEnabledIdentityProviders(
                state.getRequestIssuerEntityId(), state.isRegistering(), state.getLevelOfAssurance());

        if (!enabledIdentityProviders.contains(state.getIdentityProviderEntityId())) {
            throw new IdpDisabledException(state.getIdentityProviderEntityId());
        }

        return responseFromHubFactory.createSuccessResponseFromHub(
                state.getRequestId(),
                state.getAssertions(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri()
        );
    }
}
