package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AbstractAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

public abstract class AbstractAuthnFailedErrorStateController<S extends AbstractAuthnFailedErrorState> implements StateController, ResponsePreparedStateController, ErrorResponsePreparedStateController {

    protected S state;
    protected final StateTransitionAction stateTransitionAction;
    protected final HubEventLogger hubEventLogger;

    private final ResponseFromHubFactory responseFromHubFactory;

    public AbstractAuthnFailedErrorStateController(
            S state,
            ResponseFromHubFactory responseFromHubFactory,
            StateTransitionAction stateTransitionAction,
            HubEventLogger hubEventLogger) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
        this.stateTransitionAction = stateTransitionAction;
        this.hubEventLogger = hubEventLogger;
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
        return responseFromHubFactory.createAuthnFailedResponseFromHub(
                state.getRequestId(),
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

    protected SessionStartedState createSessionStartedState() {
        return new SessionStartedState(
                state.getRequestId(),
                state.getRelayState().orNull(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                null,
                state.getSessionExpiryTimestamp(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                true);
    }
}
