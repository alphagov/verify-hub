package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder.aUserAccountCreatedState;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountCreatedStateControllerTest {

    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Test(expected = IdpDisabledException.class)
    public void getPreparedResponse_shouldThrowWhenIdpIsDisabled() throws Exception {
        UserAccountCreatedState state = aUserAccountCreatedState().withIdentityProviderEntityId("disabled-entity-id").build();
        UserAccountCreatedStateController controller = new UserAccountCreatedStateController(state, identityProvidersConfigProxy, null);

        when(identityProvidersConfigProxy.getEnabledIdentityProviders(Matchers.<Optional<String>>any()))
                .thenReturn(Arrays.<String>asList());

        controller.getPreparedResponse();
    }

}