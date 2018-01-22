package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;

public abstract class AbstractSuccessfulMatchStateController<T extends AbstractSuccessfulMatchState> implements StateController, ResponseProcessingStateController, ResponsePreparedStateController, ErrorResponsePreparedStateController {

    protected final T state;
    protected final ResponseFromHubFactory responseFromHubFactory;

    public AbstractSuccessfulMatchStateController(
            final T state,
            final ResponseFromHubFactory responseFromHubFactory) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
    }

    public abstract ResponseFromHub getPreparedResponse();

    @Override
    public ResponseProcessingDetails getResponseProcessingDetails() {
        return new ResponseProcessingDetails(
                state.getSessionId(),
                ResponseProcessingStatus.SEND_SUCCESSFUL_MATCH_RESPONSE_TO_TRANSACTION,
                state.getRequestIssuerEntityId()
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

    protected ResponseFromHub getResponse() {
        return responseFromHubFactory.createSuccessResponseFromHub(
                state.getRequestId(),
                state.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri()
        );
    }
}
