package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Collection;

public class SuccessfulMatchStateController implements StateController, ResponseProcessingStateController, ResponsePreparedStateController, ErrorResponsePreparedStateController {

    private final SuccessfulMatchState state;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    public SuccessfulMatchStateController(
            final SuccessfulMatchState state,
            final ResponseFromHubFactory responseFromHubFactory,
            final IdentityProvidersConfigProxy identityProvidersConfigProxy) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
    }

    @Override
    public ResponseProcessingDetails getResponseProcessingDetails() {
        return new ResponseProcessingDetails(
                state.getSessionId(),
                ResponseProcessingStatus.SEND_SUCCESSFUL_MATCH_RESPONSE_TO_TRANSACTION,
                state.getRequestIssuerEntityId()
        );
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

    private ResponseFromHub getResponse() {
        return responseFromHubFactory.createSuccessResponseFromHub(
                state.getRequestId(),
                state.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri()
        );
    }

    @Override
    public ResponseFromHub getErrorResponse() {
        return responseFromHubFactory.createNoAuthnContextResponseFromHub(
                state.getRequestId(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri()
        );
    }
}
