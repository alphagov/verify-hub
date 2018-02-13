package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.FailureResponseDetails;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

public class AuthnFailedErrorStateController implements IdpSelectingStateController, StateController, ResponsePreparedStateController, ErrorResponsePreparedStateController {

    private final StateTransitionAction stateTransitionAction;
    private AuthnFailedErrorState state;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;
    private final HubEventLogger hubEventLogger;

    public AuthnFailedErrorStateController(
            AuthnFailedErrorState state,
            ResponseFromHubFactory responseFromHubFactory,
            StateTransitionAction stateTransitionAction,
            TransactionsConfigProxy transactionsConfigProxy,
            IdentityProvidersConfigProxy identityProvidersConfigProxy,
            HubEventLogger hubEventLogger) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
        this.stateTransitionAction = stateTransitionAction;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
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

    public FailureResponseDetails handleFailureResponse() {
        return new FailureResponseDetails(state.getIdpEntityId(), state.getRequestIssuerEntityId());
    }

    public void tryAnotherIdpResponse() {
        stateTransitionAction.transitionTo(createSessionStartedState());
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
                state.getTransactionSupportsEidas());
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
}
