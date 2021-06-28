package uk.gov.ida.hub.policy.domain.controller;

import com.google.inject.Injector;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder.anAuthnFailedErrorState;
import static uk.gov.ida.hub.policy.builder.state.AwaitingCycle3DataStateBuilder.anAwaitingCycle3DataState;
import static uk.gov.ida.hub.policy.builder.state.Cycle0And1MatchRequestSentStateBuilder.aCycle0And1MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.Cycle3DataInputCancelledStateBuilder.aCycle3DataInputCancelledState;
import static uk.gov.ida.hub.policy.builder.state.Cycle3MatchRequestSentStateBuilder.aCycle3MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.FraudEventDetectedStateBuilder.aFraudEventDetectedState;
import static uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder.anIdpSelectedState;
import static uk.gov.ida.hub.policy.builder.state.MatchingServiceRequestErrorStateBuilder.aMatchingServiceRequestErrorState;
import static uk.gov.ida.hub.policy.builder.state.NoMatchStateBuilder.aNoMatchState;
import static uk.gov.ida.hub.policy.builder.state.RequesterErrorStateBuilder.aRequesterErrorState;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;
import static uk.gov.ida.hub.policy.builder.state.TimeoutStateBuilder.aTimeoutState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder.aUserAccountCreatedState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationFailedStateBuilder.aUserAccountCreationFailedState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StateControllerFactoryTest {
    @Mock
    private Injector injector;

    @Mock
    private StateTransitionAction stateTransitionAction;

    private StateControllerFactory stateControllerFactory;

    @BeforeEach
    public void setUp() {
        stateControllerFactory = new StateControllerFactory(injector);
        when(injector.getInstance(AssertionRestrictionsFactory.class)).thenReturn(null);
        when(injector.getInstance(AttributeQueryService.class)).thenReturn(null);
        when(injector.getInstance(HubEventLogger.class)).thenReturn(null);
        when(injector.getInstance(IdentityProvidersConfigProxy.class)).thenReturn(null);
        when(injector.getInstance(MatchingServiceConfigProxy.class)).thenReturn(null);
        when(injector.getInstance(PolicyConfiguration.class)).thenReturn(null);
        when(injector.getInstance(ResponseFromHubFactory.class)).thenReturn(null);
        when(injector.getInstance(TransactionsConfigProxy.class)).thenReturn(null);
    }

    @Test
    public void shouldCreateASessionStartedStateController() {
        StateController controller = stateControllerFactory.build(aSessionStartedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(SessionStartedStateController.class);
    }

    @Test
    public void shouldCreateAnIdpSelectedStateController() {
        StateController controller = stateControllerFactory.build(anIdpSelectedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(IdpSelectedStateController.class);
    }

    @Test
    public void shouldCreateACycle0And1MatchRequestSentStateController() {
        StateController controller = stateControllerFactory.build(aCycle0And1MatchRequestSentState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(Cycle0And1MatchRequestSentStateController.class);
    }

    @Test
    public void shouldCreateASuccessfulMatchStateController() {
        StateController controller = stateControllerFactory.build(aSuccessfulMatchState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(SuccessfulMatchStateController.class);
    }

    @Test
    public void shouldCreateANoMatchStateController() {
        StateController controller = stateControllerFactory.build(aNoMatchState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(NoMatchStateController.class);
    }

    @Test
    public void shouldCreateAUserAccountCreatedStateController() {
        StateController controller = stateControllerFactory.build(aUserAccountCreatedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(UserAccountCreatedStateController.class);
    }

    @Test
    public void shouldCreateAnAwaitingCycle3DataStateController() {
        StateController controller = stateControllerFactory.build(anAwaitingCycle3DataState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(AwaitingCycle3DataStateController.class);
    }

    @Test
    public void shouldCreateACycle3MatchRequestSentStateController() {
        StateController controller = stateControllerFactory.build(aCycle3MatchRequestSentState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(Cycle3MatchRequestSentStateController.class);
    }

    @Test
    public void shouldCreateATimeoutStateController() {
        StateController controller = stateControllerFactory.build(aTimeoutState().build(),stateTransitionAction);

        assertThat(controller).isInstanceOf(TimeoutStateController.class);
    }

    @Test
    public void shouldCreateAMatchingServiceRequestErrorStateController() {
        StateController controller = stateControllerFactory.build(aMatchingServiceRequestErrorState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(MatchingServiceRequestErrorStateController.class);
    }

    @Test
    public void shouldCreateAUserAccountCreationRequestSentStateController() {
        StateController controller = stateControllerFactory.build(aUserAccountCreationRequestSentState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(UserAccountCreationRequestSentStateController.class);
    }

    @Test
    public void shouldCreateAnAuthnFailedErrorStateController() {
        StateController controller = stateControllerFactory.build(anAuthnFailedErrorState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(AuthnFailedErrorStateController.class);
    }

    @Test
    public void shouldCreateAFraudEventDetectedStateController() {
        StateController controller = stateControllerFactory.build(aFraudEventDetectedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(FraudEventDetectedStateController.class);
    }

    @Test
    public void shouldCreateARequesterErrorStateController() {
        StateController controller = stateControllerFactory.build(aRequesterErrorState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(RequesterErrorStateController.class);
    }

    @Test
    public void shouldCreateACycle3DataInputCancelledStateController() {
        StateController controller = stateControllerFactory.build(aCycle3DataInputCancelledState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(Cycle3DataInputCancelledStateController.class);
    }

    @Test
    public void shouldCreateAUserAccountCreationFailedStateController() {
        StateController controller = stateControllerFactory.build(aUserAccountCreationFailedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(UserAccountCreationFailedStateController.class);
    }

    @Test
    public void shouldThrowIllegalStateExceptionIfControllerIsNotFound() {
        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            UnknownState unknownState = new UnknownState(
                    "requestId",
                    "requestIssuerId",
                    DateTime.now(),
                    URI.create("/some-ac-service-uri"),
                    aSessionId().build());
            stateControllerFactory.build(unknownState, stateTransitionAction);
        });
        assertThat("Unable to locate state for UnknownState").isEqualTo(exception.getMessage());
    }

    private static class UnknownState extends AbstractState {
        public UnknownState(String requestId, String requestIssuerId, DateTime sessionExpiryTimestamp, URI assertionConsumerServiceUri, SessionId sessionId) {
            super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, null);
        }

        @Override
        public Optional<String> getRelayState() {
            return Optional.empty();
        }
    }
}
