package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.CountrySelectedStateController;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateTransitional;
import uk.gov.ida.hub.policy.exception.EidasCountryNotSupportedException;
import uk.gov.ida.hub.policy.exception.EidasNotSupportedException;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
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

    private static final EidasCountryDto COUNTRY_1 = new EidasCountryDto("id1", "country1", true);
    private static final EidasCountryDto COUNTRY_2 = new EidasCountryDto("id2", "country2", true);
    private static final EidasCountryDto DISABLED_COUNTRY = new EidasCountryDto("id3", "country3", false);

    @Before
    public void setUp() throws Exception {
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
        setSystemWideCountries(COUNTRY_1, COUNTRY_2, DISABLED_COUNTRY);
        setCountriesForRp();

        List<EidasCountryDto> countries = service.getCountries(sessionId);

        assertThat(countries, equalTo(Arrays.asList(COUNTRY_1, COUNTRY_2)));
    }

    @Test
    public void shouldReturnIntersectionOfEnabledSystemWideCountriesAndRPConfiguredCountries() {
        setSystemWideCountries(COUNTRY_1, COUNTRY_2, DISABLED_COUNTRY);
        setCountriesForRp(COUNTRY_2);

        List<EidasCountryDto> countries = service.getCountries(sessionId);

        assertThat(countries, equalTo(Arrays.asList(COUNTRY_2)));
    }

    @Test
    public void shouldSetSelectedCountry() {
        CountrySelectedStateController mockCountrySelectedStateController = mock(CountrySelectedStateController.class);
        when(sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class)).thenReturn(mockCountrySelectedStateController);
        setSystemWideCountries(COUNTRY_1);

        service.setSelectedCountry(sessionId, COUNTRY_1.getSimpleId());

        verify(mockCountrySelectedStateController).selectCountry(COUNTRY_1.getEntityId());
    }

    @Test(expected = EidasCountryNotSupportedException.class)
    public void shouldReturnErrorWhenAnInvalidCountryIsSelected() {
        CountrySelectedStateController mockCountrySelectedStateController = mock(CountrySelectedStateController.class);
        when(sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class)).thenReturn(mockCountrySelectedStateController);
        setSystemWideCountries(COUNTRY_1);

        service.setSelectedCountry(sessionId, "not-a-valid-country-code");
    }

    @Test(expected = EidasNotSupportedException.class)
    public void shouldReturnErrorWhenACountryIsSelectedWithTxnNotSupportingEidas() {
        when(sessionRepository.getTransactionSupportsEidas(sessionId)).thenReturn(false);

        service.setSelectedCountry(sessionId, "NL");
    }

    private void setSystemWideCountries(EidasCountryDto... countries) {
        when(configProxy.getEidasSupportedCountries()).thenReturn(Arrays.asList(countries));
    }

    private void setCountriesForRp(EidasCountryDto... countries) {
        when(configProxy.getEidasSupportedCountriesForRP(anyString())).thenReturn(Arrays.stream(countries).map(EidasCountryDto::getEntityId).collect(toList()));
    }
}
