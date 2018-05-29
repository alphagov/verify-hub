package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Collection;

public class UserAccountCreatedStateController implements StateController, ResponseProcessingStateController, ResponsePreparedStateController, ErrorResponsePreparedStateController {
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;
    private final UserAccountCreatedState state;
    private final ResponseFromHubFactory responseFromHubFactory;

    public UserAccountCreatedStateController(final UserAccountCreatedState state,
                                             final IdentityProvidersConfigProxy identityProvidersConfigProxy,
                                             final ResponseFromHubFactory responseFromHubFactory) {
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
    }

    @Override
    public ResponseProcessingDetails getResponseProcessingDetails() {
        return new ResponseProcessingDetails(
                state.getSessionId(),
                ResponseProcessingStatus.SEND_USER_ACCOUNT_CREATED_RESPONSE_TO_TRANSACTION,
                state.getRequestIssuerEntityId()
        );
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
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
                state.getAssertionConsumerServiceUri());
    }
}
