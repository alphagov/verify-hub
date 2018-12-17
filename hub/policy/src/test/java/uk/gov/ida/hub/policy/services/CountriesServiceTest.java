package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.EidasCountrySelectedStateController;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectingState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.exception.EidasCountryNotSupportedException;
import uk.gov.ida.hub.policy.exception.EidasNotSupportedException;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountriesServiceTest {
    @Mock
    private TransactionsConfigProxy configProxy;
    @Mock
    private SessionRepository sessionRepository;

    private CountriesService service;

    private SessionId sessionId;

    private static final String RELYING_PARTY_ID = "relyingPartyId";

    private static final EidasCountryDto COUNTRY_1 = new EidasCountryDto("id1", "country1", true);
    private static final EidasCountryDto COUNTRY_2 = new EidasCountryDto("id2", "country2", true);
    private static final EidasCountryDto DISABLED_COUNTRY = new EidasCountryDto("id3", "country3", false);

    @Before
    public void setUp() {
        service = new CountriesService(sessionRepository, configProxy);
        sessionId = SessionIdBuilder.aSessionId().with("coffee-pasta").build();
        when(sessionRepository.getTransactionSupportsEidas(sessionId)).thenReturn(true);
    }

    @Test(expected = EidasNotSupportedException.class)
    public void shouldReturnErrorWhenGettingCountriesWithTxnNotSupportingEidas() {
        when(sessionRepository.getTransactionSupportsEidas(sessionId)).thenReturn(false);

        service.getCountries(sessionId);
    }

    @Test
    public void shouldReturnEnabledSystemWideCountriesWhenRpHasNoExplicitlyEnabledCountries() {
        setSystemWideCountries(asList(COUNTRY_1, COUNTRY_2, DISABLED_COUNTRY));

        List<EidasCountryDto> countries = service.getCountries(sessionId);

        assertThat(countries, equalTo(Arrays.asList(COUNTRY_1, COUNTRY_2)));
    }

    @Test
    public void shouldReturnIntersectionOfEnabledSystemWideCountriesAndRPConfiguredCountries() {
        setSystemWideCountries(asList(COUNTRY_1, COUNTRY_2, DISABLED_COUNTRY));
        when(sessionRepository.getRequestIssuerEntityId(sessionId)).thenReturn(RELYING_PARTY_ID);
        when(configProxy.getEidasSupportedCountriesForRP(RELYING_PARTY_ID)).thenReturn(asList(COUNTRY_2.getEntityId()));
        List<EidasCountryDto> countries = service.getCountries(sessionId);

        assertThat(countries, equalTo(Arrays.asList(COUNTRY_2)));
    }

    @Test
    public void shouldSetSelectedCountry() {
        EidasCountrySelectedStateController mockEidasCountrySelectedStateController = mock(EidasCountrySelectedStateController.class);
        when(sessionRepository.getStateController(sessionId, EidasCountrySelectingState.class)).thenReturn(mockEidasCountrySelectedStateController);
        setSystemWideCountries(asList(COUNTRY_1));

        service.setSelectedCountry(sessionId, COUNTRY_1.getSimpleId());

        verify(mockEidasCountrySelectedStateController).selectCountry(COUNTRY_1.getEntityId());
    }

    @Test(expected = EidasCountryNotSupportedException.class)
    public void shouldReturnErrorWhenAnInvalidCountryIsSelected() {
        setSystemWideCountries(asList(COUNTRY_1));

        service.setSelectedCountry(sessionId, "not-a-valid-country-code");
    }

    @Test(expected = EidasNotSupportedException.class)
    public void shouldReturnErrorWhenACountryIsSelectedWithTxnNotSupportingEidas() {
        when(sessionRepository.getTransactionSupportsEidas(sessionId)).thenReturn(false);

        service.setSelectedCountry(sessionId, "NL");
    }

    private void setSystemWideCountries(List<EidasCountryDto> countryList) {
        when(configProxy.getEidasSupportedCountries()).thenReturn(countryList);
    }
}
