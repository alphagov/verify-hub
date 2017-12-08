package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;

public class TimeoutStateController implements ErrorResponsePreparedStateController {
    private TimeoutState state;
    private ResponseFromHubFactory responseFromHubFactory;

    public TimeoutStateController(TimeoutState state, ResponseFromHubFactory responseFromHubFactory) {
        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
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
