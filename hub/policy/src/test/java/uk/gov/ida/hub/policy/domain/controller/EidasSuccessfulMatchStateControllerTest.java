package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.domain.ResponseFromHubBuilder;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.services.CountriesService;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.EidasSuccessfulMatchStateBuilder.anEidasSuccessfulMatchState;

@RunWith(MockitoJUnitRunner.class)
public class EidasSuccessfulMatchStateControllerTest {

    private EidasSuccessfulMatchStateController controller;

    private EidasSuccessfulMatchState state;

    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    @Mock
    CountriesService countriesService;

    @Before
    public void setUp(){
        state = anEidasSuccessfulMatchState().withCountryEntityId("country-entity-id").build();
        controller = new EidasSuccessfulMatchStateController(state, responseFromHubFactory, countriesService);
    }

    @Test(expected = IdpDisabledException.class)
    public void getPreparedResponse_shouldThrowWhenCountryIsDisabled() {
        when(countriesService.getCountries(state.getSessionId()))
                .thenReturn(emptyList());

        controller.getPreparedResponse();
    }

    @Test
    public void shouldReturnPreparedResponse(){
        List<EidasCountryDto> enabledIdentityProviders = singletonList(new EidasCountryDto("country-entity-id", "simple-id", true));
        ResponseFromHub expectedResponseFromHub = ResponseFromHubBuilder.aResponseFromHubDto().build();
        when(countriesService.getCountries(state.getSessionId()))
                .thenReturn(enabledIdentityProviders);
        when(responseFromHubFactory.createSuccessResponseFromHub(
                state.getRequestId(),
                state.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri()))
                .thenReturn(expectedResponseFromHub);

        ResponseFromHub result = controller.getPreparedResponse();

        Assert.assertEquals(result, expectedResponseFromHub);
    }
}
