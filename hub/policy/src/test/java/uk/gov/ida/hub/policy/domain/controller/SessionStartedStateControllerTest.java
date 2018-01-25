package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;

@RunWith(MockitoJUnitRunner.class)
public class SessionStartedStateControllerTest {

    public static final String IDP_ENTITY_ID = "anIdp";

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;
    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;
    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    private SessionStartedStateController controller;

    private SessionStartedState sessionStartedState;

    @Before
    public void setup() {
        sessionStartedState = aSessionStartedState()
            .withAvailableIdpEntityIds(asList(IDP_ENTITY_ID))
            .withTransactionSupportsEidas(true)
            .build();
        when(transactionsConfigProxy.getLevelsOfAssurance(sessionStartedState.getRequestIssuerEntityId()))
                .thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, ImmutableList.of(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(idpConfigDto);
        controller = new SessionStartedStateController(
                sessionStartedState,
                eventSinkHubEventLogger,
                stateTransitionAction,
                transactionsConfigProxy,
                responseFromHubFactory,
                identityProvidersConfigProxy);
    }


    @Test
    public void handleIdpSelect_shouldTransitionStateAndLogEvent() {
        controller.handleIdpSelected(IDP_ENTITY_ID, "some-ip-address", false);
        ArgumentCaptor<IdpSelectedState> capturedState = ArgumentCaptor.forClass(IdpSelectedState.class);

        verify(stateTransitionAction, times(1)).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue().getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(capturedState.getValue().getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
        assertTrue(capturedState.getValue().getTransactionSupportsEidas());
        verify(eventSinkHubEventLogger, times(1)).logIdpSelectedEvent(capturedState.getValue(), "some-ip-address");
    }

    @Test
    public void idpSelect_shouldThrowWhenIdentityProviderInvalid() {
        try {
            controller.handleIdpSelected("notExist", "some-ip-address", false);
            fail("Should throw StateProcessingValidationException");
        }
        catch(StateProcessingValidationException e) {
            assertThat(e.getMessage()).contains("Available Identity Provider for session ID [" + sessionStartedState
                            .getSessionId().getSessionId() + "] not found for entity ID [notExist].");
            verify(eventSinkHubEventLogger, times(0)).logIdpSelectedEvent(any(IdpSelectedState.class), eq("some-ip-address"));
        }
    }
}
