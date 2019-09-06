package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

public class EidasAuthnFailedErrorStateController extends AbstractAuthnFailedErrorStateController<EidasAuthnFailedErrorState> implements RestartJourneyStateController, EidasCountrySelectingStateController {

    public EidasAuthnFailedErrorStateController(
            EidasAuthnFailedErrorState state,
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

    @Override
    public void selectCountry(final String countryEntityId) {
        EidasCountrySelectedState countrySelectedState = new EidasCountrySelectedState(
                countryEntityId,
                state.getRelayState().orElse(null),
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                state.getLevelsOfAssurance(),
                state.getForceAuthentication().orElse(null)
        );

        stateTransitionAction.transitionTo(countrySelectedState);
        hubEventLogger.logCountrySelectedEvent(countrySelectedState);
    }
}
