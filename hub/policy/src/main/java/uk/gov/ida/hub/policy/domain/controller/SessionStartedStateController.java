package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.Collections;

public class SessionStartedStateController implements IdpSelectingStateController, EidasCountrySelectingStateController, ResponseProcessingStateController, ErrorResponsePreparedStateController {

    private final SessionStartedState state;
    private final HubEventLogger hubEventLogger;
    private final StateTransitionAction stateTransitionAction;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    public SessionStartedStateController(
            final SessionStartedState state,
            final HubEventLogger hubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final TransactionsConfigProxy transactionsConfigProxy,
            final ResponseFromHubFactory responseFromHubFactory,
            final IdentityProvidersConfigProxy identityProvidersConfigProxy) {

        this.state = state;
        this.hubEventLogger = hubEventLogger;
        this.stateTransitionAction = stateTransitionAction;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.responseFromHubFactory = responseFromHubFactory;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
    }

    @Override
    public AuthnRequestSignInProcess getSignInProcessDetails() {
        return new AuthnRequestSignInProcess(
                state.getRequestIssuerEntityId(),
                state.getTransactionSupportsEidas());
    }

    @Override
    public String getRequestIssuerId() {
        return state.getRequestIssuerEntityId();
    }

    @Override
    public void handleIdpSelected(final String idpEntityId, final String principalIpAddress, boolean registering, LevelOfAssurance requestedLoa, String analyticsSessionId, String journeyType, String abTestVariant) {
        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, idpEntityId, registering, requestedLoa, transactionsConfigProxy, identityProvidersConfigProxy);
        stateTransitionAction.transitionTo(idpSelectedState);
        hubEventLogger.logIdpSelectedEvent(idpSelectedState, principalIpAddress, analyticsSessionId, journeyType, abTestVariant);
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
        EidasCountrySelectedState eidasCountrySelectedState = new EidasCountrySelectedState(
                countryEntityId,
                state.getRelayState().orElse(null),
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                Collections.singletonList(LevelOfAssurance.LEVEL_2), // TODO: EID-154 will plug in a real LOA
                state.getForceAuthentication().orElse(null)
        );
        stateTransitionAction.transitionTo(eidasCountrySelectedState);
        hubEventLogger.logCountrySelectedEvent(eidasCountrySelectedState);
    }
}
