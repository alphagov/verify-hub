package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.AuthenticationErrorResponse;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.FraudFromIdp;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.RequesterErrorResponse;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.SuccessFromIdp;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.PausedRegistrationState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Optional.fromNullable;
import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.wrongResponseIssuer;

public class IdpSelectedStateController implements StateController, ErrorResponsePreparedStateController, IdpSelectingStateController, AuthnRequestCapableController {

    private final IdpSelectedState state;
    private final SessionStartedStateFactory sessionStartedStateFactory;
    private final EventSinkHubEventLogger eventSinkHubEventLogger;
    private final StateTransitionAction stateTransitionAction;
    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final AssertionRestrictionsFactory assertionRestrictionFactory;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final PolicyConfiguration policyConfiguration;

    public IdpSelectedStateController(
            final IdpSelectedState state,
            final SessionStartedStateFactory sessionStartedStateFactory,
            final EventSinkHubEventLogger eventSinkHubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final IdentityProvidersConfigProxy identityProvidersConfigProxy,
            final TransactionsConfigProxy transactionsConfigProxy,
            final ResponseFromHubFactory responseFromHubFactory,
            final PolicyConfiguration policyConfiguration,
            final AssertionRestrictionsFactory assertionRestrictionsFactory,
            final MatchingServiceConfigProxy matchingServiceConfigProxy) {

        this.state = state;
        this.sessionStartedStateFactory = sessionStartedStateFactory;
        this.eventSinkHubEventLogger = eventSinkHubEventLogger;
        this.stateTransitionAction = stateTransitionAction;
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.responseFromHubFactory = responseFromHubFactory;
        this.assertionRestrictionFactory = assertionRestrictionsFactory;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.policyConfiguration = policyConfiguration;
    }

    public AuthnRequestFromHub getRequestFromHub() {
        AuthnRequestFromHub requestToSendFromHub = new AuthnRequestFromHub(
                state.getRequestId(),
                state.getLevelsOfAssurance(),
                state.getUseExactComparisonType(),
                state.getIdpEntityId(),
                state.getForceAuthentication(),
                state.getSessionExpiryTimestamp(),
                state.registering(),
                null);

        eventSinkHubEventLogger.logRequestFromHub(state.getSessionId(), state.getRequestIssuerEntityId());
        return requestToSendFromHub;
    }

    private void validateIdpIsEnabledAndWasIssuedWithRequest(String entityId, String requestIssuerEntityId) {
        Collection<String> enabledIdentityProviders = identityProvidersConfigProxy.getEnabledIdentityProviders(
                fromNullable(requestIssuerEntityId));

        if (!enabledIdentityProviders.contains(entityId)) {
            throw new IdpDisabledException(entityId);
        }

        if (!entityId.equals(state.getIdpEntityId())) {
            throw wrongResponseIssuer(state.getRequestId(), entityId, state.getIdpEntityId());
        }
    }

    private void validateIdpLevelOfAssuranceIsInAcceptedLevels(LevelOfAssurance responseLevel, List<LevelOfAssurance> acceptedLevels) {
        if (!acceptedLevels.contains(responseLevel)) {
            throw StateProcessingValidationException.wrongLevelOfAssurance(Optional.ofNullable(responseLevel), acceptedLevels);
        }
    }

    private void validateReturnedLevelOfAssuranceFromIdpIsConsistentWithIdpConfig(LevelOfAssurance levelOfAssurance, String issuer, String requestId) {
        if(!identityProvidersConfigProxy.getIdpConfig(issuer).getSupportedLevelsOfAssurance().contains(levelOfAssurance)) {
            throw StateProcessingValidationException.idpReturnedUnsupportedLevelOfAssurance(levelOfAssurance, requestId, issuer);
        }
    }

    public void handleNoAuthenticationContextResponseFromIdp(AuthenticationErrorResponse authenticationErrorResponse) {
        validateIdpIsEnabledAndWasIssuedWithRequest(authenticationErrorResponse.getIssuer(), state.getRequestIssuerEntityId());
        eventSinkHubEventLogger.logNoAuthnContextEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getSessionExpiryTimestamp(), state.getRequestId(), authenticationErrorResponse.getPrincipalIpAddressAsSeenByHub());
        if (state.registering()) {
            stateTransitionAction.transitionTo(createAuthnFailedErrorState());
        } else {
            stateTransitionAction.transitionTo(createSessionStartedState());
        }
    }

    public void handlePausedRegistrationResponseFromIdp(String requestIssuerEntityId, String principalIdAsSeenByHub) {
        validateIdpIsEnabledAndWasIssuedWithRequest(requestIssuerEntityId, state.getRequestIssuerEntityId());
        eventSinkHubEventLogger.logPausedRegistrationEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getSessionExpiryTimestamp(), state.getRequestId(), principalIdAsSeenByHub);
        stateTransitionAction.transitionTo(createPausedRegistrationState());
    }

    public void handleAuthenticationFailedResponseFromIdp(AuthenticationErrorResponse authenticationErrorResponse) {
        validateIdpIsEnabledAndWasIssuedWithRequest(authenticationErrorResponse.getIssuer(), state.getRequestIssuerEntityId());
        eventSinkHubEventLogger.logIdpAuthnFailedEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getSessionExpiryTimestamp(), state.getRequestId(), authenticationErrorResponse.getPrincipalIpAddressAsSeenByHub());
        stateTransitionAction.transitionTo(createAuthnFailedErrorState());
    }

    public void handleRequesterErrorResponseFromIdp(RequesterErrorResponse requesterErrorResponseDto) {
        validateIdpIsEnabledAndWasIssuedWithRequest(requesterErrorResponseDto.getIssuer(), state.getRequestIssuerEntityId());
        eventSinkHubEventLogger.logIdpRequesterErrorEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId(),
                requesterErrorResponseDto.getErrorMessage(),
                requesterErrorResponseDto.getPrincipalIpAddressAsSeenByHub());
        stateTransitionAction.transitionTo(createRequesterErrorState());
    }

    public void handleSuccessResponseFromIdp(SuccessFromIdp successFromIdp) {
        validateIdpIsEnabledAndWasIssuedWithRequest(successFromIdp.getIssuer(), state.getRequestIssuerEntityId());
        validateIdpLevelOfAssuranceIsInAcceptedLevels(successFromIdp.getLevelOfAssurance(), state.getLevelsOfAssurance());
        validateReturnedLevelOfAssuranceFromIdpIsConsistentWithIdpConfig(successFromIdp.getLevelOfAssurance(), successFromIdp.getIssuer(), state.getRequestId());
        eventSinkHubEventLogger.logIdpAuthnSucceededEvent(
                state.getSessionId(),
                state.getSessionExpiryTimestamp(),
                state.getIdpEntityId(),
                state.getRequestIssuerEntityId(),
                successFromIdp.getPersistentId(),
                state.getRequestId(),
                state.getLevelsOfAssurance().get(0),
                state.getLevelsOfAssurance().get(state.getLevelsOfAssurance().size() - 1),
                successFromIdp.getLevelOfAssurance(),
                successFromIdp.getPrincipalIpAddressAsSeenByIdp(),
                successFromIdp.getPrincipalIpAddressAsSeenByHub());

        stateTransitionAction.transitionTo(createCycle0And1MatchRequestSentState(successFromIdp));
    }

    public void handleFraudResponseFromIdp(FraudFromIdp fraudFromIdp) {
        validateIdpIsEnabledAndWasIssuedWithRequest(fraudFromIdp.getIssuer(), state.getRequestIssuerEntityId());
        eventSinkHubEventLogger.logIdpFraudEvent(
                state.getSessionId(),
                state.getIdpEntityId(),
                state.getRequestIssuerEntityId(),
                fraudFromIdp.getPersistentId(),
                state.getSessionExpiryTimestamp(),
                fraudFromIdp.getFraudDetectedDetails(),
                fraudFromIdp.getPrincipalIpAddressAsSeenByIdp(),
                fraudFromIdp.getPrincipalIpAddressSeenByHub(),
                state.getRequestId());

        stateTransitionAction.transitionTo(createFraudEventDetectedState());
    }

    private State createRequesterErrorState() {
        return new RequesterErrorState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getForceAuthentication(),
                state.getTransactionSupportsEidas());
    }

    private State createFraudEventDetectedState() {
        return new FraudEventDetectedState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getIdpEntityId(),
                state.getForceAuthentication(),
                state.getTransactionSupportsEidas());
    }

    private SessionStartedState createSessionStartedState() {
        return sessionStartedStateFactory.build(
                state.getRequestId(),
                state.getAssertionConsumerServiceUri(),
                state.getRequestIssuerEntityId(),
                state.getRelayState(),
                state.getForceAuthentication(),
                state.getSessionExpiryTimestamp(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
    }

    private State createAuthnFailedErrorState() {
        return new AuthnFailedErrorState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getIdpEntityId(),
                state.getForceAuthentication(),
                state.getTransactionSupportsEidas());
    }

    private State createPausedRegistrationState() {
        return new PausedRegistrationState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                state.getRelayState());
    }

    private State createCycle0And1MatchRequestSentState(SuccessFromIdp successFromIdp) {
        String authnStatementAssertion = successFromIdp.getAuthnStatementAssertion();

        String matchingServiceEntityId = getMatchingServiceEntityId();

        return new Cycle0And1MatchRequestSentState(
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getAssertionConsumerServiceUri(),
            new SessionId(state.getSessionId().getSessionId()),
            state.getTransactionSupportsEidas(),
            successFromIdp.getIssuer(),
            state.getRelayState(),
            successFromIdp.getLevelOfAssurance(),
            matchingServiceEntityId,
            successFromIdp.getEncryptedMatchingDatasetAssertion(),
            authnStatementAssertion,
            successFromIdp.getPersistentId()
        );
    }

    public AttributeQueryRequestDto createAttributeQuery(SuccessFromIdp successFromIdp) {
        String authnStatementAssertion = successFromIdp.getAuthnStatementAssertion();
        final String encryptedMatchingDatasetAssertion = successFromIdp.getEncryptedMatchingDatasetAssertion();

        String matchingServiceEntityId = getMatchingServiceEntityId();
        MatchingServiceConfigEntityDataDto matchingServiceConfig = matchingServiceConfigProxy.getMatchingService(matchingServiceEntityId);
        return AttributeQueryRequestDto.createCycle01MatchingServiceRequest(
                state.getRequestId(),
                encryptedMatchingDatasetAssertion,
                authnStatementAssertion,
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                matchingServiceEntityId,
                DateTime.now().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()),
                successFromIdp.getLevelOfAssurance(),
                successFromIdp.getPersistentId(),
                assertionRestrictionFactory.getAssertionExpiry(),
                matchingServiceConfig.getUri(),
                matchingServiceConfig.isOnboarding());

    }

    @Override
    public ResponseFromHub getErrorResponse() {
        return responseFromHubFactory.createNoAuthnContextResponseFromHub(
                state.getRequestId(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri());
    }

    public boolean isRegistrationContext() {
        return state.registering();
    }

    public String getMatchingServiceEntityId() {
        return Optional
                .ofNullable(state.getMatchingServiceEntityId())
                .orElse(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId()));
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
