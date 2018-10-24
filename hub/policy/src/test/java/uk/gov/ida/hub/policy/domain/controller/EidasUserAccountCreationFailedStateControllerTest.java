package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.state.EidasUserAccountCreationFailedStateBuilder.aEidasUserAccountCreationFailedState;

@RunWith(MockitoJUnitRunner.class)
public class EidasUserAccountCreationFailedStateControllerTest {

    private static final String REQUEST_ISSUER_ID = "requestIssuerId";

    private EidasUserAccountCreationFailedState eidasUserAccountCreationFailedState;

    private EidasUserAccountCreationFailedStateController controller;

    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private HubEventLogger hubEventLogger;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    @Before
    public void setUp() {
        eidasUserAccountCreationFailedState = aEidasUserAccountCreationFailedState().withRequestIssuerId(REQUEST_ISSUER_ID).build();

        controller = new EidasUserAccountCreationFailedStateController(
                eidasUserAccountCreationFailedState,
                responseFromHubFactory,
                stateTransitionAction,
                hubEventLogger);
    }

    @Test
    public void shouldTransitionToSessionStartedStateAndLogEvent() {
        controller.transitionToSessionStartedState();
        ArgumentCaptor<SessionStartedState> capturedState = ArgumentCaptor.forClass(SessionStartedState.class);

        verify(stateTransitionAction, times(1)).transitionTo(capturedState.capture());
        verify(hubEventLogger, times(1)).logSessionMovedToStartStateEvent(capturedState.getValue());

        assertThat(capturedState.getValue().getSessionId()).isEqualTo(eidasUserAccountCreationFailedState.getSessionId());
        assertThat(capturedState.getValue().getRequestIssuerEntityId()).isEqualTo(REQUEST_ISSUER_ID);
        assertThat(capturedState.getValue().getTransactionSupportsEidas()).isEqualTo(true);
        assertThat(capturedState.getValue().getForceAuthentication().orNull()).isEqualTo(false);
    }
}
