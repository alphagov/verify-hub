package uk.gov.ida.hub.policy.services;

import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.EidasCountrySelectingStateController;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectingState;
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
        EidasCountryDto selectedCountry = supportedCountries.stream()
            .filter(country -> country.getSimpleId().equals(countryCode))
            .findFirst()
            .orElseThrow(() -> new EidasCountryNotSupportedException(sessionId, countryCode));

        EidasCountrySelectingStateController eidasCountrySelectingStateController = (EidasCountrySelectingStateController) sessionRepository.getStateController(sessionId, EidasCountrySelectingState.class);
        eidasCountrySelectingStateController.selectCountry(selectedCountry.getEntityId());
    }

    private void ensureTransactionSupportsEidas(uk.gov.ida.hub.policy.domain.SessionId sessionId) {
        sessionRepository.validateSessionExists(sessionId);
        if( !sessionRepository.getTransactionSupportsEidas(sessionId) ) {
            throw new uk.gov.ida.hub.policy.exception.EidasNotSupportedException(sessionId);
        }
    }
}
