package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.FailureResponseDetails;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

public class AuthnFailedErrorStateController implements IdpSelectingStateController, StateController, ResponsePreparedStateController, ErrorResponsePreparedStateController {

    private final StateTransitionAction stateTransitionAction;
    private final SessionStartedStateFactory sessionStartedStateFactory;
    private AuthnFailedErrorState state;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;
    private final EventSinkHubEventLogger eventSinkHubEventLogger;

    public AuthnFailedErrorStateController(
            AuthnFailedErrorState state,
            ResponseFromHubFactory responseFromHubFactory,
            StateTransitionAction stateTransitionAction,
            SessionStartedStateFactory sessionStartedStateFactory,
            TransactionsConfigProxy transactionsConfigProxy,
            IdentityProvidersConfigProxy identityProvidersConfigProxy,
            EventSinkHubEventLogger eventSinkHubEventLogger) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
        this.sessionStartedStateFactory = sessionStartedStateFactory;
        this.stateTransitionAction = stateTransitionAction;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
        this.eventSinkHubEventLogger = eventSinkHubEventLogger;
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
        return sessionStartedStateFactory.build(
                state.getRequestId(),
                state.getAssertionConsumerServiceUri(),
                state.getRequestIssuerEntityId(),
                state.getRelayState(),
                Optional.<Boolean>absent(),
                state.getSessionExpiryTimestamp(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
    }

    @Override
    public void handleIdpSelected(String idpEntityId, String principalIpAddress, boolean registering) {
        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, idpEntityId, registering, transactionsConfigProxy, identityProvidersConfigProxy);
        stateTransitionAction.transitionTo(idpSelectedState);
        eventSinkHubEventLogger.logIdpSelectedEvent(idpSelectedState, principalIpAddress);
    }

    @Override
    public String getRequestIssuerId() {
        return state.getRequestIssuerEntityId();
    }

    @Override
    public AuthnRequestSignInProcess getSignInProcessDetails() {
        return new AuthnRequestSignInProcess(
                state.getAvailableIdentityProviderEntityIds(),
                state.getRequestIssuerEntityId(),
                state.getTransactionSupportsEidas());
    }
}
