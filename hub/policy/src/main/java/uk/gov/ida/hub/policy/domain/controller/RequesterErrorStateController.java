package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.*;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

public class RequesterErrorStateController implements StateController, ResponsePreparedStateController, ErrorResponsePreparedStateController, IdpSelectingStateController {

    private final RequesterErrorState state;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final StateTransitionAction stateTransitionAction;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;
    private final EventSinkHubEventLogger eventSinkHubEventLogger;

    public RequesterErrorStateController(
        RequesterErrorState state,
        ResponseFromHubFactory responseFromHubFactory,
        StateTransitionAction stateTransitionAction,
        TransactionsConfigProxy transactionsConfigProxy,
        IdentityProvidersConfigProxy identityProvidersConfigProxy,
        EventSinkHubEventLogger eventSinkHubEventLogger) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
        this.stateTransitionAction = stateTransitionAction;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
        this.eventSinkHubEventLogger = eventSinkHubEventLogger;
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
        return responseFromHubFactory.createRequesterErrorResponseFromHub(
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

    @Override
    public void handleIdpSelected(String idpEntityId, String principalIpAddress, boolean registering, LevelOfAssurance requestedLoa) {
        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, idpEntityId, registering, requestedLoa, transactionsConfigProxy, identityProvidersConfigProxy);
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
            state.getRequestIssuerEntityId(),
            state.getTransactionSupportsEidas());
    }
}
