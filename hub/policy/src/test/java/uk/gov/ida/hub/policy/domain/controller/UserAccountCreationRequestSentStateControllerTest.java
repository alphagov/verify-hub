package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromMatchingService;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentStateTransitional;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountCreationRequestSentStateControllerTest {

    @Mock
    public EventSinkHubEventLogger eventSinkHubEventLogger;

    @Mock
    public LevelOfAssuranceValidator levelOfAssuranceValidator;

    @Test
    public void getNextState_shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() throws Exception {
        UserAccountCreationRequestSentStateTransitional state = aUserAccountCreationRequestSentState().build();
        UserAccountCreationRequestSentStateController controller =
                new UserAccountCreationRequestSentStateController(state, null, null, null, null, null, null);

        ResponseFromMatchingService responseFromMatchingService = new UserAccountCreatedFromMatchingService("issuer-id", "", "", Optional.<LevelOfAssurance>absent());

        try {
            controller.validateResponse(responseFromMatchingService);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo("Response to request ID [" + state.getRequestId() + "] came from [issuer-id] and was expected to come from [matchingServiceEntityId]");
        }
    }

    @Test
    public void getNextState_shouldMaintainRelayState() throws Exception {
        final String relayState = "4x100m";
        UserAccountCreationRequestSentStateTransitional state = aUserAccountCreationRequestSentState()
                .withRelayState(Optional.of(relayState))
                .build();
        UserAccountCreationRequestSentStateController controller =
                new UserAccountCreationRequestSentStateController(state, null, eventSinkHubEventLogger, null, levelOfAssuranceValidator, null, null);

        UserAccountCreatedFromMatchingService userAccountCreatedFromMatchingService = new UserAccountCreatedFromMatchingService("issuer-id", "", "", Optional.<LevelOfAssurance>absent());

        final State newState = controller.getNextStateForUserAccountCreated(userAccountCreatedFromMatchingService);
        assertThat(newState).isInstanceOf(UserAccountCreatedStateTransitional.class);

        final UserAccountCreatedStateTransitional userAccountCreatedState = (UserAccountCreatedStateTransitional)newState;
        assertThat(userAccountCreatedState.getRelayState()).isNotNull();
        assertThat(userAccountCreatedState.getRelayState().isPresent()).isTrue();
        assertThat(userAccountCreatedState.getRelayState().get()).isEqualTo(relayState);

    }
}
