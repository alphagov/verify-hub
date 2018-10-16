package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.FailureResponseDetails;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

public class AuthnFailedErrorStateController extends AbstractAuthnFailedErrorStateController<AuthnFailedErrorState> implements IdpSelectingStateController {

    private final TransactionsConfigProxy transactionsConfigProxy;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    public AuthnFailedErrorStateController(
            AuthnFailedErrorState state,
            ResponseFromHubFactory responseFromHubFactory,
            StateTransitionAction stateTransitionAction,
            TransactionsConfigProxy transactionsConfigProxy,
            IdentityProvidersConfigProxy identityProvidersConfigProxy,
            HubEventLogger hubEventLogger) {

        super(state, responseFromHubFactory, stateTransitionAction, hubEventLogger);

        this.transactionsConfigProxy = transactionsConfigProxy;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
    }

    public FailureResponseDetails handleFailureResponse() {
        return new FailureResponseDetails(state.getIdpEntityId(), state.getRequestIssuerEntityId());
    }

    public void tryAnotherIdpResponse() {
        stateTransitionAction.transitionTo(createSessionStartedState());
    }

    @Override
    public void handleIdpSelected(String idpEntityId, String principalIpAddress, boolean registering, LevelOfAssurance requestedLoa) {
        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, idpEntityId, registering, requestedLoa, transactionsConfigProxy, identityProvidersConfigProxy);
        stateTransitionAction.transitionTo(idpSelectedState);
        hubEventLogger.logIdpSelectedEvent(idpSelectedState, principalIpAddress);
    }

    @Override
    public String getRequestIssuerId() {
        return state.getRequestIssuerEntityId();
    }

    @Override
    public AuthnRequestSignInProcess getSignInProcessDetails() {
        return new AuthnRequestSignInProcess(
                state.getRequestIssuerEntityId(),
                state.getTransactionSupportsEidas());
    }

    protected SessionStartedState createSessionStartedState() {
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
