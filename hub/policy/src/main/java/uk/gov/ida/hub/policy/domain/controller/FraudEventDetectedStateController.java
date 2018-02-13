package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

public class FraudEventDetectedStateController extends AuthnFailedErrorStateController {

    public FraudEventDetectedStateController(
            FraudEventDetectedState state,
            ResponseFromHubFactory responseFromHubFactory,
            StateTransitionAction stateTransitionAction,
            TransactionsConfigProxy transactionsConfigProxy,
            IdentityProvidersConfigProxy identityProvidersConfigProxy,
            HubEventLogger hubEventLogger) {

        super(
                state,
                responseFromHubFactory,
                stateTransitionAction,
                transactionsConfigProxy,
                identityProvidersConfigProxy,
                hubEventLogger);
    }
}
