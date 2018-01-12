package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;

@RunWith(MockitoJUnitRunner.class)
public class SuccessfulMatchStateControllerTest {

    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Test(expected = IdpDisabledException.class)
    public void getPreparedResponse_shouldThrowWhenIdpIsDisabled() throws Exception {
        final SuccessfulMatchState state = aSuccessfulMatchState().build();
        SuccessfulMatchStateController controller = new SuccessfulMatchStateController(state, mock(ResponseFromHubFactory.class), identityProvidersConfigProxy);
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(Matchers.any(String.class), Matchers.anyBoolean(), Matchers.any(LevelOfAssurance.class)))
                .thenReturn(emptyList());

        controller.getPreparedResponse();
    }
}
