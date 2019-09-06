package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.ResponseFromMatchingService;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountCreationRequestSentStateControllerTest {

    @Mock
    public HubEventLogger hubEventLogger;

    @Mock
    public LevelOfAssuranceValidator levelOfAssuranceValidator;

    @Mock
    public StateTransitionAction stateTransitionAction;

    @Test
    public void shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() {
        UserAccountCreationRequestSentState state = aUserAccountCreationRequestSentState().build();
        UserAccountCreationRequestSentStateController controller =
                new UserAccountCreationRequestSentStateController(state, null, null, null, null, null, null, null, null);

        ResponseFromMatchingService responseFromMatchingService = new UserAccountCreatedFromMatchingService("issuer-id", "", "", Optional.empty());

        try {
            controller.validateResponse(responseFromMatchingService);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo("Response to request ID [" + state.getRequestId() + "] came from [issuer-id] and was expected to come from [matchingServiceEntityId]");
        }
    }

    @Test
    public void shouldMaintainRelayStateForUserAccountCreatedResponseFromMatchingService() {
        final ArgumentCaptor<State> capturedState = ArgumentCaptor.forClass(State.class);
        final String relayState = "4x100m";
        UserAccountCreationRequestSentState state = aUserAccountCreationRequestSentState()
                .withRelayState(relayState)
                .build();
        UserAccountCreationRequestSentStateController controller =
                new UserAccountCreationRequestSentStateController(state, stateTransitionAction, hubEventLogger, null, levelOfAssuranceValidator, null, null, null, null);

        UserAccountCreatedFromMatchingService userAccountCreatedFromMatchingService = new UserAccountCreatedFromMatchingService(
                state.getMatchingServiceAdapterEntityId(), state.getRequestId(), "matchingServiceAssertion", Optional.of(state.getIdpLevelOfAssurance()));

        controller.handleUserAccountCreatedResponseFromMatchingService(userAccountCreatedFromMatchingService);

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isInstanceOf(UserAccountCreatedState.class);

        final UserAccountCreatedState userAccountCreatedState = (UserAccountCreatedState) capturedState.getValue();
        assertThat(userAccountCreatedState.getRelayState()).isNotNull();
        assertThat(userAccountCreatedState.getRelayState().isPresent()).isTrue();
        assertThat(userAccountCreatedState.getRelayState().get()).isEqualTo(relayState);
    }
}
