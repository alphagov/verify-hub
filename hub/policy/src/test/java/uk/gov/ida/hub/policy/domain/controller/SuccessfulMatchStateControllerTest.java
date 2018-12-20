package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.domain.ResponseFromHubBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;

@RunWith(MockitoJUnitRunner.class)
public class SuccessfulMatchStateControllerTest {

    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    private SuccessfulMatchStateController controller;
    private SuccessfulMatchState state;


    @Before
    public void setUp(){
        state = aSuccessfulMatchState().build();
        controller = new SuccessfulMatchStateController(state, responseFromHubFactory, identityProvidersConfigProxy);
    }

    @Test(expected = IdpDisabledException.class)
    public void getPreparedResponse_shouldThrowWhenIdpIsDisabled() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(any(String.class), anyBoolean(), any(LevelOfAssurance.class)))
                .thenReturn(emptyList());

        controller.getPreparedResponse();
    }

    @Test
    public void getPreparedResponse_shouldReturnResponse(){
        List<String> enabledIdentityProviders = singletonList(state.getIdentityProviderEntityId());
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
