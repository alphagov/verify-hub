package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedStateTransitional;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder.aUserAccountCreatedState;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountCreatedStateControllerTest {

    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Test(expected = IdpDisabledException.class)
    public void getPreparedResponse_shouldThrowWhenIdpIsDisabled() throws Exception {
        UserAccountCreatedStateTransitional state = aUserAccountCreatedState().withIdentityProviderEntityId("disabled-entity-id").build();
        UserAccountCreatedStateController controller = new UserAccountCreatedStateController(state, identityProvidersConfigProxy, null);

        when(identityProvidersConfigProxy.getEnabledIdentityProviders(Matchers.any(String.class), Matchers.anyBoolean(), Matchers.any(LevelOfAssurance.class)))
                .thenReturn(emptyList());

        controller.getPreparedResponse();
    }
}
