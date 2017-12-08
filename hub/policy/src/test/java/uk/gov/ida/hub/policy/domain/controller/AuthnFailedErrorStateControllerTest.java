package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder.anAuthnFailedErrorState;

@RunWith(MockitoJUnitRunner.class)
public class AuthnFailedErrorStateControllerTest {

    private static final String IDP_ENTITY_ID = "anIdp";

    private AuthnFailedErrorState authnFailedErrorState;

    private AuthnFailedErrorStateController controller;

    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;
    @Mock
    private SessionStartedStateFactory sessionStartedStateFactory;

    @Before
    public void setup() {
        authnFailedErrorState = anAuthnFailedErrorState()
            .withAvailableIdpEntityIds(asList(IDP_ENTITY_ID))
            .withTransactionSupportsEidas(true)
            .build();
        when(transactionsConfigProxy.getLevelsOfAssurance(authnFailedErrorState.getRequestIssuerEntityId()))
                .thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(idpConfigDto);
        controller = new AuthnFailedErrorStateController(
                authnFailedErrorState,
                responseFromHubFactory,
                stateTransitionAction,
                sessionStartedStateFactory,
                transactionsConfigProxy,
                identityProvidersConfigProxy,
                eventSinkHubEventLogger);
    }

    @Test
    public void handleIdpSelect_shouldTransitionStateAndLogEvent() {
        controller.handleIdpSelected(IDP_ENTITY_ID, "some-ip-address", false);
        ArgumentCaptor<IdpSelectedState> capturedState = ArgumentCaptor.forClass(IdpSelectedState.class);

        verify(stateTransitionAction, times(1)).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue().getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(capturedState.getValue().getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
        verify(eventSinkHubEventLogger, times(1)).logIdpSelectedEvent(capturedState.getValue(), "some-ip-address");
    }

    @Test
    public void idpSelect_shouldThrowWhenIdentityProviderInvalid() {
        try {
            controller.handleIdpSelected("notExist", "some-ip-address", false);
            fail("Should throw StateProcessingValidationException");
        }
        catch(StateProcessingValidationException e) {
            assertThat(e.getMessage()).contains("Available Identity Provider for session ID [" + authnFailedErrorState
                    .getSessionId().getSessionId() + "] not found for entity ID [notExist].");
            verify(eventSinkHubEventLogger, times(0)).logIdpSelectedEvent(any(IdpSelectedState.class), eq("some-ip-address"));
        }
    }

    @Test
    public void getSignInProcessDetails_shouldReturnFieldsFromTheState() throws Exception {
        AuthnRequestSignInProcess signInProcessDetails = controller.getSignInProcessDetails();
        assertThat(signInProcessDetails.getTransactionSupportsEidas()).isEqualTo(true);
        assertThat(signInProcessDetails.getAvailableIdentityProviderEntityIds()).containsSequence("anIdp");
        assertThat(signInProcessDetails.getRequestIssuerId()).isEqualTo("requestIssuerId");
    }

}
