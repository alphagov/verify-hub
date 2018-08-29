package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.CountryAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.state.CountryAuthnFailedErrorStateBuilder.aCountryAuthnFailedErrorState;

@RunWith(MockitoJUnitRunner.class)
public class CountryAuthnFailedErrorStateControllerTest {

    private static final String REQUEST_ISSUER_ID = "requestIssuerId";

    private CountryAuthnFailedErrorState countryAuthnFailedErrorState;

    private CountryAuthnFailedErrorStateController controller;

    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private HubEventLogger hubEventLogger;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    @Before
    public void setup() {
        countryAuthnFailedErrorState = aCountryAuthnFailedErrorState().withRequestIssuerId(REQUEST_ISSUER_ID).build();

        controller = new CountryAuthnFailedErrorStateController(
                countryAuthnFailedErrorState,
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

        assertThat(capturedState.getValue().getSessionId()).isEqualTo(countryAuthnFailedErrorState.getSessionId());
        assertThat(capturedState.getValue().getRequestIssuerEntityId()).isEqualTo(REQUEST_ISSUER_ID);
        assertThat(capturedState.getValue().getTransactionSupportsEidas()).isEqualTo(true);
    }
}
