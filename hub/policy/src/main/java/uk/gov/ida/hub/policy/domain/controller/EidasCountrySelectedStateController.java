package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.Collections;
import java.util.Optional;

public class EidasCountrySelectedStateController implements ErrorResponsePreparedStateController, EidasCountrySelectingStateController, AuthnRequestCapableController, RestartJourneyStateController {

    private final EidasCountrySelectedState state;
    private final HubEventLogger hubEventLogger;
    private final StateTransitionAction stateTransitionAction;
    private final PolicyConfiguration policyConfiguration;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final AssertionRestrictionsFactory assertionRestrictionFactory;

    public EidasCountrySelectedStateController(
            final EidasCountrySelectedState state,
            final HubEventLogger hubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final PolicyConfiguration policyConfiguration,
            final TransactionsConfigProxy transactionsConfigProxy,
            final MatchingServiceConfigProxy matchingServiceConfigProxy,
            final ResponseFromHubFactory responseFromHubFactory,
            final AssertionRestrictionsFactory assertionRestrictionFactory) {
        this.state = state;
        this.hubEventLogger = hubEventLogger;
        this.stateTransitionAction = stateTransitionAction;
        this.policyConfiguration = policyConfiguration;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.responseFromHubFactory = responseFromHubFactory;
        this.assertionRestrictionFactory = assertionRestrictionFactory;
    }

    public String getMatchingServiceEntityId() {
        return transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId());
    }

    public String getRequestIssuerId() {
        return state.getRequestIssuerEntityId();
    }

    public boolean isMatchingJourney() {
        return transactionsConfigProxy.isUsingMatching(state.getRequestIssuerEntityId());
    }

    public String getCountryEntityId() {
        return state.getCountryEntityId();
    }

    public AuthnRequestFromHub getRequestFromHub() {
        Optional<EidasCountryDto> countryDto = this.transactionsConfigProxy.getEidasSupportedCountries().stream()
                .filter(eidasCountryDto -> eidasCountryDto.getEntityId().equals(state.getCountryEntityId())).findFirst();

        AuthnRequestFromHub requestToSendFromHub = new AuthnRequestFromHub(
                state.getRequestId(),
                state.getLevelsOfAssurance(),
                false,
                state.getCountryEntityId(),
                Optional.of(true),
                state.getSessionExpiryTimestamp(),
                false,
                countryDto.map(EidasCountryDto::getOverriddenSsoUrl).orElse(null));

        hubEventLogger.logRequestFromHub(state.getSessionId(), state.getRequestIssuerEntityId());
        return requestToSendFromHub;
    }

    public EidasAttributeQueryRequestDto getEidasAttributeQueryRequestDto(InboundResponseFromCountry translatedResponse) {
        validateSuccessfulResponse(translatedResponse);

        final String matchingServiceEntityId = getMatchingServiceEntityId();
        MatchingServiceConfigEntityDataDto matchingServiceConfig = matchingServiceConfigProxy.getMatchingService(matchingServiceEntityId);

        return new EidasAttributeQueryRequestDto(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                assertionRestrictionFactory.getAssertionExpiry(),
                matchingServiceEntityId,
                matchingServiceConfig.getUri(),
                DateTime.now().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()),
                matchingServiceConfig.isOnboarding(),
                translatedResponse.getLevelOfAssurance().get(),
                new PersistentId(translatedResponse.getPersistentId().get()),
                Optional.empty(),
                Optional.empty(),
                translatedResponse.getEncryptedIdentityAssertionBlob().get(),
                translatedResponse.getUnsignedAssertions()
        );
    }

    @Override
    public ResponseFromHub getErrorResponse() {
        return responseFromHubFactory.createNoAuthnContextResponseFromHub(
                state.getRequestId(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri());
    }

    @Override
    public void selectCountry(String countryEntityId) {
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

    private void handleSuccessResponseFromCountry(InboundResponseFromCountry translatedResponse,
                                                  String principalIpAddressAsSeenByHub,
                                                  String analyticsSessionId,
                                                  String journeyType) {
        validateSuccessfulResponse(translatedResponse);

        hubEventLogger.logIdpAuthnSucceededEvent(
                state.getSessionId(),
                state.getSessionExpiryTimestamp(),
                state.getCountryEntityId(),
                state.getRequestIssuerEntityId(),
                new PersistentId(translatedResponse.getPersistentId().get()),
                state.getRequestId(),
                state.getLevelsOfAssurance().get(0),
                state.getLevelsOfAssurance().get(state.getLevelsOfAssurance().size() - 1),
                translatedResponse.getLevelOfAssurance().get(),
                Optional.empty(),
                principalIpAddressAsSeenByHub,
                analyticsSessionId,
                journeyType);
    }

    public void handleMatchingJourneySuccessResponseFromCountry(InboundResponseFromCountry translatedResponse,
                                                                String principalIpAddressAsSeenByHub,
                                                                String analyticsSessionId,
                                                                String journeyType) {
        handleSuccessResponseFromCountry(translatedResponse, principalIpAddressAsSeenByHub, analyticsSessionId, journeyType);
        stateTransitionAction.transitionTo(createEidasCycle0And1MatchRequestSentState(translatedResponse));
    }

    public void handleNonMatchingJourneySuccessResponseFromCountry(InboundResponseFromCountry translatedResponse,
                                                                   String principalIpAddressAsSeenByHub,
                                                                   String analyticsSessionId,
                                                                   String journeyType) {
        handleSuccessResponseFromCountry(translatedResponse, principalIpAddressAsSeenByHub, analyticsSessionId, journeyType);
        stateTransitionAction.transitionTo(createNonMatchingJourneySuccessState(translatedResponse));
    }

    public void handleAuthenticationFailedResponseFromCountry(String principalIpAddressAsSeenByHub,
                                                              String analyticsSessionId,
                                                              String journeyType) {
        hubEventLogger.logIdpAuthnFailedEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId(),
                principalIpAddressAsSeenByHub,
                analyticsSessionId,
                journeyType
        );

        stateTransitionAction.transitionTo(createEidasAuthnFailedErrorState());
    }

    private void validateSuccessfulResponse(InboundResponseFromCountry translatedResponse) {
        if (!translatedResponse.getPersistentId().isPresent()) {
            throw StateProcessingValidationException.missingMandatoryAttribute(state.getRequestId(), "persistentId");
        }

        if (!translatedResponse.getEncryptedIdentityAssertionBlob().isPresent()) {
            throw StateProcessingValidationException.missingMandatoryAttribute(state.getRequestId(), "encryptedIdentityAssertionBlob");
        }

        if (!translatedResponse.getLevelOfAssurance().isPresent()) {
            throw StateProcessingValidationException.noLevelOfAssurance();
        } else {
            validateLevelOfAssurance(translatedResponse.getLevelOfAssurance().get());
        }
    }

    private void validateLevelOfAssurance(LevelOfAssurance loa) {
        if (!state.getLevelsOfAssurance().contains(loa)) {
            throw StateProcessingValidationException.wrongLevelOfAssurance(Optional.ofNullable(loa), state.getLevelsOfAssurance());
        }
    }

    private State createEidasCycle0And1MatchRequestSentState(final InboundResponseFromCountry translatedResponse) {
        return new EidasCycle0And1MatchRequestSentState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                new SessionId(state.getSessionId().getSessionId()),
                state.getTransactionSupportsEidas(),
                translatedResponse.getIssuer(),
                state.getRelayState().orElse(null),
                translatedResponse.getLevelOfAssurance().get(),
                getMatchingServiceEntityId(),
                translatedResponse.getEncryptedIdentityAssertionBlob().get(),
                new PersistentId(translatedResponse.getPersistentId().get()),
                state.getForceAuthentication().orElse(null)
        );
    }

    private State createNonMatchingJourneySuccessState(final InboundResponseFromCountry translatedResponse) {
        return new NonMatchingJourneySuccessState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                new SessionId(state.getSessionId().getSessionId()),
                state.getTransactionSupportsEidas(),
                state.getRelayState().orElse(null),
                translatedResponse.getEncryptedIdentityAssertionBlob().map(Collections::singleton).orElse(Collections.emptySet())
        );
    }

    private State createEidasAuthnFailedErrorState() {
        return new EidasAuthnFailedErrorState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState().orElse(null),
                state.getSessionId(),
                state.getCountryEntityId(),
                state.getLevelsOfAssurance(),
                state.getForceAuthentication().orElse(null));
    }

    @Override
    public void transitionToSessionStartedState() {
        final SessionStartedState sessionStartedState = createSessionStartedState();
        hubEventLogger.logSessionMovedToStartStateEvent(sessionStartedState);
        stateTransitionAction.transitionTo(sessionStartedState);
    }

    private SessionStartedState createSessionStartedState() {
        return new SessionStartedState(
                state.getRequestId(),
                state.getRelayState().orElse(null),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getForceAuthentication().orElse(null),
                state.getSessionExpiryTimestamp(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
    }
}
