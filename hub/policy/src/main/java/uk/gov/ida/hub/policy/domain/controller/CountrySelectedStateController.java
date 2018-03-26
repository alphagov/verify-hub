package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class CountrySelectedStateController implements StateController, ErrorResponsePreparedStateController, CountrySelectingStateController, AuthnRequestCapableController {
    private final CountrySelectedState state;
    private final HubEventLogger hubEventLogger;
    private final StateTransitionAction stateTransitionAction;
    private final TransactionsConfigProxy transactionsConfigProxy;

    @Override
    public ResponseFromHub getErrorResponse() {
        return null;
    }


    public CountrySelectedStateController(
            final CountrySelectedState state,
            final HubEventLogger hubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final TransactionsConfigProxy transactionsConfigProxy) {
        this.state = state;
        this.hubEventLogger = hubEventLogger;
        this.stateTransitionAction = stateTransitionAction;
        this.transactionsConfigProxy = transactionsConfigProxy;
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
                state.getLevelsOfAssurance()
        );
        stateTransitionAction.transitionTo(countrySelectedState);
        hubEventLogger.logCountrySelectedEvent(countrySelectedState);
    }

    public void validateCountryIsIn(List<EidasCountryDto> countries) {
        if (countries.stream().filter(c -> state.getCountryEntityId().equals(c.getEntityId())).count() == 0) {
            throw StateProcessingValidationException.eidasCountryNotEnabled(state.getCountryEntityId());
        }
    }

    public void validateLevelOfAssurance(LevelOfAssurance loa) {
        if (!state.getLevelsOfAssurance().contains(loa)) {
            throw StateProcessingValidationException.wrongLevelOfAssurance(Optional.ofNullable(loa), state.getLevelsOfAssurance());
        }
    }

    public AuthnRequestFromHub getRequestFromHub() {
        // Use TransactionConfigProxy to lookup list of countries and findFirst matching by entityId
        Optional<EidasCountryDto> countryDto = this.transactionsConfigProxy.getEidasSupportedCountries()
                .stream()
                .filter(eidasCountryDto -> eidasCountryDto.getEntityId().equals(state.getCountryEntityId()))
                .findFirst();

        AuthnRequestFromHub requestToSendFromHub = new AuthnRequestFromHub(
                state.getRequestId(),
                state.getLevelsOfAssurance(),
                false,
                state.getCountryEntityId(),
                com.google.common.base.Optional.of(true),
                state.getSessionExpiryTimestamp(),
                false,
                countryDto.map(EidasCountryDto::getOverriddenSsoUrl).orElse(null));

        hubEventLogger.logRequestFromHub(state.getSessionId(), state.getRequestIssuerEntityId());
        return requestToSendFromHub;
    }

    // TODO: Add to check if state has matching service entity id
    public String getMatchingServiceEntityId() {
        return transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId());
    }

    public String getRequestId() {
        return state.getRequestId();
    }

    public URI getAssertionConsumerServiceUri() {
        return state.getAssertionConsumerServiceUri();
    }

    public String getRequestIssuerEntityId() {
        return state.getRequestIssuerEntityId();
    }

    public void transitionToEidasCycle0And1MatchRequestSentState(
        final EidasAttributeQueryRequestDto dto,
        final String principalIpAddressAsSeenByHub,
        final String identityProviderEntityId) {
        hubEventLogger.logIdpAuthnSucceededEvent(
            state.getSessionId(),
            state.getSessionExpiryTimestamp(),
            state.getCountryEntityId(),
            state.getRequestIssuerEntityId(),
            dto.getPersistentId(),
            state.getRequestId(),
            state.getLevelsOfAssurance().get(0),
            state.getLevelsOfAssurance().get(state.getLevelsOfAssurance().size() - 1),
            dto.getLevelOfAssurance(),
            com.google.common.base.Optional.absent(),
            principalIpAddressAsSeenByHub);

        stateTransitionAction.transitionTo(createEidasCycle0And1MatchRequestSentState(dto, identityProviderEntityId));
    }

    private State createEidasCycle0And1MatchRequestSentState(
        final EidasAttributeQueryRequestDto dto,
        final String identityProviderEntityId) {
        return new EidasCycle0And1MatchRequestSentState(
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getAssertionConsumerServiceUri(),
            new SessionId(state.getSessionId().getSessionId()),
            state.getTransactionSupportsEidas(),
            identityProviderEntityId,
            state.getRelayState().orNull(),
            dto.getLevelOfAssurance(),
            getMatchingServiceEntityId(),
            dto.getEncryptedIdentityAssertion(),
            dto.getPersistentId()
        );
    }

}
