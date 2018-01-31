package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.domain.ResponseFromHubBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchStateTransitional;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;

@RunWith(MockitoJUnitRunner.class)
public class SuccessfulMatchStateControllerTest {

    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    private SuccessfulMatchStateController controller;
    private SuccessfulMatchStateTransitional state;


    @Before
    public void setUp(){
        state = aSuccessfulMatchState().build();
        controller = new SuccessfulMatchStateController(state, responseFromHubFactory, identityProvidersConfigProxy);
    }

    @Test(expected = IdpDisabledException.class)
    public void getPreparedResponse_shouldThrowWhenIdpIsDisabled() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(Matchers.any(String.class), Matchers.anyBoolean(), Matchers.any(LevelOfAssurance.class)))
                .thenReturn(emptyList());

        controller.getPreparedResponse();
    }

    @Test
    public void getPreparedResponse_shouldReturnResponse(){
        List<String> enabledIdentityProviders = Arrays.asList(state.getIdentityProviderEntityId());
        ResponseFromHub expectedResponseFromHub = ResponseFromHubBuilder.aResponseFromHubDto().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(eq(state.getRequestIssuerEntityId()), anyBoolean(), any(LevelOfAssurance.class)))
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
