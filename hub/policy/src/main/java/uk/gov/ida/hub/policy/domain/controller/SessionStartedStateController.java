package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.Collections;

public class SessionStartedStateController implements IdpSelectingStateController, CountrySelectingStateController, ResponseProcessingStateController, ErrorResponsePreparedStateController {

    private final SessionStartedState state;
    private final EventSinkHubEventLogger eventSinkHubEventLogger;
    private final StateTransitionAction stateTransitionAction;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    public SessionStartedStateController(
            final SessionStartedState state,
            final EventSinkHubEventLogger eventSinkHubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final TransactionsConfigProxy transactionsConfigProxy,
            final ResponseFromHubFactory responseFromHubFactory,
            final IdentityProvidersConfigProxy identityProvidersConfigProxy) {

        this.state = state;
        this.eventSinkHubEventLogger = eventSinkHubEventLogger;
        this.stateTransitionAction = stateTransitionAction;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.responseFromHubFactory = responseFromHubFactory;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
    }

    @Override
    public AuthnRequestSignInProcess getSignInProcessDetails() {
        return new AuthnRequestSignInProcess(
                state.getAvailableIdentityProviderEntityIds(),
                state.getRequestIssuerEntityId(),
                state.getTransactionSupportsEidas());
    }

    @Override
    public String getRequestIssuerId() {
        return state.getRequestIssuerEntityId();
    }

    @Override
    public void handleIdpSelected(final String idpEntityId, final String principalIpAddress, boolean registering, LevelOfAssurance requestedLoa) {
        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, idpEntityId, registering, requestedLoa, transactionsConfigProxy, identityProvidersConfigProxy);
        stateTransitionAction.transitionTo(idpSelectedState);
        eventSinkHubEventLogger.logIdpSelectedEvent(idpSelectedState, principalIpAddress);
    }

    @Override
    public ResponseProcessingDetails getResponseProcessingDetails() {
        // This is set to no-authn context based on the fact that the only reason we should be getting here is via the
        // No authn context response from the idp
        return new ResponseProcessingDetails(
                state.getSessionId(),
                ResponseProcessingStatus.GOTO_HUB_LANDING_PAGE,
                state.getRequestIssuerEntityId()
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
    public void selectCountry(String countryEntityId) {
        CountrySelectedState countrySelectedState = new CountrySelectedState(
                countryEntityId,
                state.getRelayState(),
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                Collections.singletonList(LevelOfAssurance.LEVEL_2) // TODO: EID-154 will plug in a real LOA
        );
        stateTransitionAction.transitionTo(countrySelectedState);
        eventSinkHubEventLogger.logCountrySelectedEvent(countrySelectedState);
    }
}
