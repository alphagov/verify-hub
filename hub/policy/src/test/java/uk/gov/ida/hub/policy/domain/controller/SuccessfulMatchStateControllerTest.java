package uk.gov.ida.hub.policy.domain.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;

@ExtendWith(MockitoExtension.class)
public class SuccessfulMatchStateControllerTest {

    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    private SuccessfulMatchStateController controller;
    private SuccessfulMatchState state;


    @BeforeEach
    public void setUp() {
        state = aSuccessfulMatchState().build();
        controller = new SuccessfulMatchStateController(state, responseFromHubFactory, identityProvidersConfigProxy);
    }

    @Test
    public void getPreparedResponse_shouldThrowWhenIdpIsDisabled() {
        Assertions.assertThrows(IdpDisabledException.class, () -> {
            when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationResponseProcessing(any(String.class), anyBoolean(), any(LevelOfAssurance.class)))
                    .thenReturn(emptyList());

            controller.getPreparedResponse();
        });
    }

    @Test
    public void getPreparedResponse_shouldReturnResponse() {
        final List<String> enabledIdentityProviders = singletonList(state.getIdentityProviderEntityId());
        final ResponseFromHub expectedResponseFromHub = ResponseFromHubBuilder.aResponseFromHubDto().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationResponseProcessing(eq(state.getRequestIssuerEntityId()), anyBoolean(), any(LevelOfAssurance.class)))
                .thenReturn(enabledIdentityProviders);
        when(responseFromHubFactory.createSuccessResponseFromHub(
                state.getRequestId(),
                state.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri()))
                .thenReturn(expectedResponseFromHub);

        final ResponseFromHub result = controller.getPreparedResponse();

        assertThat(result).isEqualTo(expectedResponseFromHub);
    }
}
