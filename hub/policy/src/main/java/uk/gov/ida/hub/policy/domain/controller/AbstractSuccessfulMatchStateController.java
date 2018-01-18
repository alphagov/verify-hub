package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Collection;

import static com.google.common.base.Optional.fromNullable;

public abstract class AbstractSuccessfulMatchStateController implements StateController, ResponseProcessingStateController, ResponsePreparedStateController, ErrorResponsePreparedStateController {

    protected final AbstractSuccessfulMatchState state;
    protected final ResponseFromHubFactory responseFromHubFactory;
    protected final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    public AbstractSuccessfulMatchStateController(
            final AbstractSuccessfulMatchState state,
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


    public abstract ResponseFromHub getPreparedResponse();

    protected ResponseFromHub getResponse() {
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
