package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

public class FraudEventDetectedStateController extends AuthnFailedErrorStateController {

    public FraudEventDetectedStateController(
            FraudEventDetectedStateTransitional state,
            ResponseFromHubFactory responseFromHubFactory,
            StateTransitionAction stateTransitionAction,
            SessionStartedStateFactory sessionStartedStateFactory,
            TransactionsConfigProxy transactionsConfigProxy,
            IdentityProvidersConfigProxy identityProvidersConfigProxy,
            EventSinkHubEventLogger eventSinkHubEventLogger) {

        super(state, responseFromHubFactory, stateTransitionAction, sessionStartedStateFactory, transactionsConfigProxy,
                identityProvidersConfigProxy, eventSinkHubEventLogger);
    }
}
