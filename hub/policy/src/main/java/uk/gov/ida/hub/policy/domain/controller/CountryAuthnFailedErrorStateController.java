package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.CountryAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

public class CountryAuthnFailedErrorStateController extends AbstractAuthnFailedErrorStateController<CountryAuthnFailedErrorState> implements EidasUnsuccessfulJourneyStateController {

    public CountryAuthnFailedErrorStateController(
            CountryAuthnFailedErrorState state,
            ResponseFromHubFactory responseFromHubFactory,
            StateTransitionAction stateTransitionAction,
            HubEventLogger hubEventLogger) {

        super(state, responseFromHubFactory, stateTransitionAction, hubEventLogger);
    }

    @Override
    public void transitionToSessionStartedState() {
        final SessionStartedState sessionStartedState = createSessionStartedState();
        hubEventLogger.logSessionMovedToStartStateEvent(sessionStartedState);
        stateTransitionAction.transitionTo(sessionStartedState);
    }
}
