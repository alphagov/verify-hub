package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

public class UserAccountCreationFailedStateController extends AbstractUserAccountCreationFailedStateController<UserAccountCreationFailedState> implements RestartJourneyStateController {

    private StateTransitionAction stateTransitionAction;
    private HubEventLogger hubEventLogger;

    public UserAccountCreationFailedStateController(
            final UserAccountCreationFailedState state,
            final ResponseFromHubFactory responseFromHubFactory,
            final StateTransitionAction stateTransitionAction,
            final HubEventLogger hubEventLogger) {

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
                state.getForceAuthentication().orNull(),
                state.getSessionExpiryTimestamp(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
    }
}
