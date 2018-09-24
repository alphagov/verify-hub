package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseFromMatchingService;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.state.EidasUserAccountCreationFailedStateBuilder.aEidasUserAccountCreationFailedState;
import static uk.gov.ida.hub.policy.builder.state.EidasSuccessfulMatchStateBuilder.anEidasSuccessfulMatchState;
import static uk.gov.ida.hub.policy.builder.state.EidasUserAccountCreationRequestSentStateBuilder.anEidasUserAccountCreationRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder.aUserAccountCreatedState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState;

@RunWith(MockitoJUnitRunner.class)
public class EidasUserAccountCreationRequestSentStateControllerTest {

    private EidasUserAccountCreationRequestSentState state;

    private EidasUserAccountCreationRequestSentStateController controller;

    @Mock
    public HubEventLogger hubEventLogger;
    @Mock
    public LevelOfAssuranceValidator levelOfAssuranceValidator;
    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private PolicyConfiguration policyConfiguration;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;
    @Mock
    private AttributeQueryService attributeQueryService;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    @Before
    public void setUp() {
        state = anEidasUserAccountCreationRequestSentState().build();

        controller = new EidasUserAccountCreationRequestSentStateController(
                state,
                stateTransitionAction,
                hubEventLogger,
                policyConfiguration,
                levelOfAssuranceValidator,
                responseFromHubFactory,
                attributeQueryService,
                transactionsConfigProxy,
                matchingServiceConfigProxy);
    }

    @Test
    public void shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() {
        ResponseFromMatchingService responseFromMatchingService = new UserAccountCreatedFromMatchingService("issuer-id", "", "", Optional.absent());

        try {
            controller.validateResponse(responseFromMatchingService);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo("Response to request ID [" + state.getRequestId() + "] came from [issuer-id] and was expected to come from [matchingServiceEntityId]");
        }
    }

    @Test
    public void shouldMaintainRelayState() {
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

    @Test
    public void shouldReturnEidasSuccessfulMatchState() {
        final String matchingServiceAssertion = "matchingServiceAssertion";

        final EidasSuccessfulMatchState expectedState = anEidasSuccessfulMatchState()
                .withCountryEntityId(state.getIdentityProviderEntityId())
                .withMatchingServiceAssertion(matchingServiceAssertion)
                .withRelayState(state.getRelayState().orNull())
                .withRequestId(state.getRequestId())
                .withRequestIssuerId(state.getRequestIssuerEntityId())
                .withSessionExpiryTimestamp(state.getSessionExpiryTimestamp())
                .withAssertionConsumerServiceUri(state.getAssertionConsumerServiceUri())
                .withSessionId(state.getSessionId())
                .build();

        final EidasSuccessfulMatchState successfulMatchState = controller.createSuccessfulMatchState(matchingServiceAssertion);

        assertThat(successfulMatchState).isInstanceOf(EidasSuccessfulMatchState.class);
        assertThat(successfulMatchState).isEqualToComparingFieldByField(expectedState);
    }

    @Test
    public void shouldLogAndReturnUserAccountCreatedStateWhenReceivedUserAccountCreatedResponse() {
        final ArgumentCaptor<State> capturedState = ArgumentCaptor.forClass(State.class);
        final String matchingServiceAssertion = "matchingServiceAssertion";

        final UserAccountCreatedState expectedState = aUserAccountCreatedState()
                .withAssertionConsumerServiceUri(state.getAssertionConsumerServiceUri())
                .withIdentityProviderEntityId(state.getIdentityProviderEntityId())
                .withSessionExpiryTimestamp(state.getSessionExpiryTimestamp())
                .withMatchingServiceAssertion(matchingServiceAssertion)
                .withRequestIssuerId(state.getRequestIssuerEntityId())
                .withTransactionSupportsEidas(true)
                .withRequestId(state.getRequestId())
                .withSessionId(state.getSessionId())
                .build();

        controller.handleUserAccountCreatedResponseFromMatchingService(new UserAccountCreatedFromMatchingService(
                state.getMatchingServiceAdapterEntityId(), state.getRequestId(), matchingServiceAssertion, Optional.of(state.getIdpLevelOfAssurance())));

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        verify(hubEventLogger, times(1)).logUserAccountCreatedEvent(
                state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());

        assertThat(capturedState.getValue()).isInstanceOf(UserAccountCreatedState.class);
        assertThat(capturedState.getValue()).isEqualToComparingFieldByField(expectedState);
    }

    @Test
    public void shouldLogAndTransitionToEidasUserAccountCreationFailedStateWhenReceivedUserAccountCreationFailedResponse() {
        final ArgumentCaptor<EidasUserAccountCreationFailedState> capturedState = ArgumentCaptor.forClass(EidasUserAccountCreationFailedState.class);

        final EidasUserAccountCreationFailedState expectedState = aEidasUserAccountCreationFailedState()
                .withSessionExpiryTimestamp(state.getSessionExpiryTimestamp())
                .withRequestIssuerId(state.getRequestIssuerEntityId())
                .withRequestid(state.getRequestId())
                .withSessionId(state.getSessionId())
                .build();

        controller.handleUserAccountCreationFailedResponseFromMatchingService();

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        verify(hubEventLogger, times(1)).logUserAccountCreationFailedEvent(
                state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());

        assertThat(capturedState.getValue()).isInstanceOf(EidasUserAccountCreationFailedState.class);
        assertThat(capturedState.getValue()).isEqualToComparingFieldByField(expectedState);
    }
}
