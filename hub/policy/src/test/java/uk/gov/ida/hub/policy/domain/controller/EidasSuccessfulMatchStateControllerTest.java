package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.domain.ResponseFromHubBuilder;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.services.CountriesService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.EidasSuccessfulMatchStateBuilder.anEidasSuccessfulMatchState;

@RunWith(MockitoJUnitRunner.class)
public class EidasSuccessfulMatchStateControllerTest {

    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    private EidasSuccessfulMatchStateController controller;

    private EidasSuccessfulMatchState state;

    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    @Mock
    CountriesService countriesService;


    @Before
    public void setUp(){
        state = anEidasSuccessfulMatchState().withIdentityProviderEntityId("country-entity-id").build();
        controller = new EidasSuccessfulMatchStateController(state, responseFromHubFactory, identityProvidersConfigProxy, countriesService);
    }

    @Test(expected = IdpDisabledException.class)
    public void getPreparedResponse_shouldThrowWhenCountryIsDisabled() {
        when(countriesService.getCountries(state.getSessionId()))
                .thenReturn(Arrays.asList());

        controller.getPreparedResponse();
    }

    @Test
    public void getPreparedResponse_shouldReturnResponse(){
        List<EidasCountryDto> enabledIdentityProviders = Arrays.asList(new EidasCountryDto("country-entity-id", null, true, null));
        ResponseFromHub expectedResponseFromHub = ResponseFromHubBuilder.aResponseFromHubDto().build();
        when(countriesService.getCountries(state.getSessionId()))
                .thenReturn(enabledIdentityProviders);
        when(responseFromHubFactory.createSuccessResponseFromHub(any(), any(), any(), any(), any()))
                .thenReturn(expectedResponseFromHub);

        ResponseFromHub result = controller.getPreparedResponse();

        Assert.assertEquals(result, expectedResponseFromHub);

    }
}
