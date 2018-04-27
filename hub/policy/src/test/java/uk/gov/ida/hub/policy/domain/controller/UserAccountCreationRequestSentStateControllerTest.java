package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.ResponseFromMatchingService;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountCreationRequestSentStateControllerTest {

    @Mock
    public HubEventLogger hubEventLogger;

    @Mock
    public LevelOfAssuranceValidator levelOfAssuranceValidator;

    @Test
    public void getNextState_shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() {
        UserAccountCreationRequestSentState state = aUserAccountCreationRequestSentState().build();
        UserAccountCreationRequestSentStateController controller =
                new UserAccountCreationRequestSentStateController(state, null, null, null, null, null, null, null, null);

        ResponseFromMatchingService responseFromMatchingService = new UserAccountCreatedFromMatchingService("issuer-id", "", "", Optional.absent());

        try {
            controller.validateResponse(responseFromMatchingService);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo("Response to request ID [" + state.getRequestId() + "] came from [issuer-id] and was expected to come from [matchingServiceEntityId]");
        }
    }

    @Test
    public void getNextState_shouldMaintainRelayState() {
        final String relayState = "4x100m";
        UserAccountCreationRequestSentState state = aUserAccountCreationRequestSentState()
                .withRelayState(relayState)
                .build();
        UserAccountCreationRequestSentStateController controller =
                new UserAccountCreationRequestSentStateController(state, null, hubEventLogger, null, levelOfAssuranceValidator, null, null, null, null);

        UserAccountCreatedFromMatchingService userAccountCreatedFromMatchingService = new UserAccountCreatedFromMatchingService("issuer-id", "", "", Optional.absent());

        final State newState = controller.getNextStateForUserAccountCreated(userAccountCreatedFromMatchingService);
        assertThat(newState).isInstanceOf(UserAccountCreatedState.class);

        final UserAccountCreatedState userAccountCreatedState = (UserAccountCreatedState)newState;
        assertThat(userAccountCreatedState.getRelayState()).isNotNull();
        assertThat(userAccountCreatedState.getRelayState().isPresent()).isTrue();
        assertThat(userAccountCreatedState.getRelayState().get()).isEqualTo(relayState);

    }
}
