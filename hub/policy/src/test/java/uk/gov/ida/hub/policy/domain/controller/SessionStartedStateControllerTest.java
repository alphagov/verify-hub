package uk.gov.ida.hub.policy.domain.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SessionStartedStateControllerTest {

    private static final String IDP_ENTITY_ID = "anIdp";
    private static final String AB_TEST_VARIANT = "variantA";
    private static final boolean REGISTERING = false;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;
    @Mock
    private HubEventLogger hubEventLogger;
    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    private SessionStartedStateController controller;

    private SessionStartedState sessionStartedState;

    @BeforeEach
    public void setup() {
        sessionStartedState = aSessionStartedState()
                .withForceAuthentication(false)
                .build();
        when(transactionsConfigProxy.getLevelsOfAssurance(sessionStartedState.getRequestIssuerEntityId()))
                .thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, List.of(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(idpConfigDto);
        when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationRequestGeneration(sessionStartedState.getRequestIssuerEntityId(), REGISTERING, LevelOfAssurance.LEVEL_2))
                .thenReturn(singletonList(IDP_ENTITY_ID));
        controller = new SessionStartedStateController(
                sessionStartedState,
                hubEventLogger,
                stateTransitionAction,
                transactionsConfigProxy,
                responseFromHubFactory,
                identityProvidersConfigProxy);
    }

    @Test
    public void handleIdpSelect_shouldTransitionStateAndLogEvent() {
        controller.handleIdpSelected(IDP_ENTITY_ID, "some-ip-address", REGISTERING, LevelOfAssurance.LEVEL_2, "some-analytics-session-id", "some-journey-id", AB_TEST_VARIANT);
        ArgumentCaptor<IdpSelectedState> capturedState = ArgumentCaptor.forClass(IdpSelectedState.class);

        verify(stateTransitionAction, times(1)).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue().getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(capturedState.getValue().getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
        verify(hubEventLogger, times(1)).logIdpSelectedEvent(capturedState.getValue(), "some-ip-address", "some-analytics-session-id", "some-journey-id", AB_TEST_VARIANT);
    }

    @Test
    public void idpSelect_shouldThrowWhenIdentityProviderInvalid() {
        try {
            controller.handleIdpSelected("notExist", "some-ip-address", false, LevelOfAssurance.LEVEL_2, "some-analytics-session-id", "some-journey-id", AB_TEST_VARIANT);
            fail("Should throw StateProcessingValidationException");
        }
        catch(StateProcessingValidationException e) {
            assertThat(e.getMessage()).contains("Available Identity Provider for session ID [" + sessionStartedState
                    .getSessionId().getSessionId() + "] not found for entity ID [notExist].");
            verify(hubEventLogger, times(0)).logIdpSelectedEvent(any(IdpSelectedState.class), eq("some-ip-address"),eq("some-analytics-session-id"), eq("some-journey-id"), eq(AB_TEST_VARIANT));
        }
    }

}
