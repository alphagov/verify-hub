package uk.gov.ida.hub.policy.services;

import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.controller.CountrySelectingStateController;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.exception.EidasCountryNotSupportedException;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CountriesService {
    private final TransactionsConfigProxy configProxy;
    private final SessionRepository sessionRepository;

    @Inject
    public CountriesService(SessionRepository sessionRepository, TransactionsConfigProxy configProxy) {
        this.sessionRepository = sessionRepository;
        this.configProxy = configProxy;
    }

    public List<EidasCountryDto> getCountries(SessionId sessionId) {
        ensureTransactionSupportsEidas(sessionId);

        List<EidasCountryDto> eidasSupportedCountries = configProxy.getEidasSupportedCountries();

        String relyingPartyId = sessionRepository.getRequestIssuerEntityId(sessionId);
        List<String> rpSpecificCountries = configProxy.getEidasSupportedCountriesForRP(relyingPartyId);

        return eidasSupportedCountries.stream()
                .filter(EidasCountryDto::getEnabled)
                .filter(country -> rpSpecificCountries.isEmpty() || rpSpecificCountries.contains(country.getEntityId()))
                .collect(Collectors.toList());
    }

    public void setSelectedCountry(SessionId sessionId, String countryCode) {
        ensureTransactionSupportsEidas(sessionId);

        List<EidasCountryDto> supportedCountries = getCountries(sessionId);
        Optional<EidasCountryDto> selectedCountry = supportedCountries.stream()
            .filter(country -> country.getSimpleId().equals(countryCode))
            .findFirst();
        if ( !selectedCountry.isPresent() ) {
            throw new EidasCountryNotSupportedException(sessionId, countryCode);
        }

        Class<? extends State> expectedStateClass = SessionStartedState.class;
        if (sessionRepository.isSessionInState(sessionId, CountrySelectedState.class)) {
            expectedStateClass = CountrySelectedState.class;
        }

        CountrySelectingStateController countrySelectingStateController = (CountrySelectingStateController) sessionRepository.getStateController(sessionId, expectedStateClass);
        countrySelectingStateController.selectCountry(selectedCountry.get().getEntityId());
    }

    private void ensureTransactionSupportsEidas(uk.gov.ida.hub.policy.domain.SessionId sessionId) {
        sessionRepository.validateSessionExists(sessionId);
        if( !sessionRepository.getTransactionSupportsEidas(sessionId) ) {
            throw new uk.gov.ida.hub.policy.exception.EidasNotSupportedException(sessionId);
        }
    }
}
