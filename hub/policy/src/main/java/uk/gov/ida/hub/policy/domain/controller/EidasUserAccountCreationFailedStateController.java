package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

public class EidasUserAccountCreationFailedStateController extends AbstractUserAccountCreationFailedStateController<EidasUserAccountCreationFailedState> implements EidasUnsuccessfulJourneyStateController {

    private StateTransitionAction stateTransitionAction;
    private HubEventLogger hubEventLogger;

    public EidasUserAccountCreationFailedStateController(
            final EidasUserAccountCreationFailedState state,
            final ResponseFromHubFactory responseFromHubFactory,
            final StateTransitionAction stateTransitionAction,
            HubEventLogger hubEventLogger) {

        super(state, responseFromHubFactory);

        this.stateTransitionAction = stateTransitionAction;
        this.hubEventLogger = hubEventLogger;
    }

    @Override
    public void transitionToSessionStartedState() {
        final SessionStartedState sessionStartedState = createSessionStartedState();
        hubEventLogger.logSessionMovedToStartStateEvent(sessionStartedState);
        stateTransitionAction.transitionTo(sessionStartedState);
    }

    private SessionStartedState createSessionStartedState() {
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
