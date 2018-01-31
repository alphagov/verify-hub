package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.AwaitingCycle3DataStateBuilder;
import uk.gov.ida.hub.policy.builder.state.Cycle0And1MatchRequestSentStateBuilder;
import uk.gov.ida.hub.policy.builder.state.Cycle3MatchRequestSentStateBuilder;
import uk.gov.ida.hub.policy.builder.state.FraudEventDetectedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.RequesterErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder;
import uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataStateTransitional;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.ResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchStateTransitional;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentStateTransitional;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateException;
import uk.gov.ida.hub.policy.exception.SessionTimeoutException;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Optional.absent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;

@RunWith(MockitoJUnitRunner.class)
public class SessionRepositoryTest {

    private SessionRepository sessionRepository;
    private ConcurrentMap<SessionId, State> dataStore;
    private ConcurrentMap<SessionId, DateTime> sessionStartedMap;
    private DateTime defaultSessionExpiry = DateTime.now().plusDays(8);

    @Mock
    private StateControllerFactory controllerFactory;

    @Captor
    private ArgumentCaptor<StateTransitionAction> stateTransitionActionArgumentCaptor = null;

    @Captor
    private ArgumentCaptor<TimeoutState> timeoutStateArgumentCaptor = null;

    @Before
    public void setup() {
        dataStore = new ConcurrentHashMap<>();
        sessionStartedMap = new ConcurrentHashMap<>();
        sessionRepository = new SessionRepository(dataStore, sessionStartedMap, controllerFactory);
    }

    @Deprecated
    @Test
    public void testIdpSelectedStateNewIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final IdpSelectedState idpSelectedState = IdpSelectedStateBuilder.anIdpSelectedState().withSessionId(expectedSessionId).buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, idpSelectedState);

        sessionRepository.getStateController(expectedSessionId, IdpSelectedStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(idpSelectedState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, IdpSelectedState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(idpSelectedState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void testSessionStartedStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedStateNew = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final SessionStartedState sessionStartedState = aSessionStartedState().withSessionId(expectedSessionId).buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedStateNew);
        dataStore.replace(sessionId, sessionStartedStateNew, sessionStartedState);

        sessionRepository.getStateController(expectedSessionId, SessionStartedStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(sessionStartedState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, SessionStartedState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(sessionStartedState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void authnFailedErrorStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final AuthnFailedErrorState authnFailedErrorState = AuthnFailedErrorStateBuilder.anAuthnFailedErrorState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, authnFailedErrorState);

        sessionRepository.getStateController(expectedSessionId, AuthnFailedErrorStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(authnFailedErrorState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, AuthnFailedErrorState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(authnFailedErrorState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void fraudEventDetectedStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final FraudEventDetectedState fraudEventDetectedState = FraudEventDetectedStateBuilder.aFraudEventDetectedState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, fraudEventDetectedState);

        sessionRepository.getStateController(expectedSessionId, FraudEventDetectedStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(fraudEventDetectedState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, FraudEventDetectedState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(fraudEventDetectedState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void requesterErrorStateStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final RequesterErrorState requesterErrorState = RequesterErrorStateBuilder.aRequesterErrorState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, requesterErrorState);

        sessionRepository.getStateController(expectedSessionId, RequesterErrorStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(requesterErrorState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, RequesterErrorState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(requesterErrorState), any(StateTransitionAction.class));
    }


    @Deprecated
    @Test
    public void awaitingCycle3DataStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final AwaitingCycle3DataState awaitingCycle3DataState = AwaitingCycle3DataStateBuilder.anAwaitingCycle3DataState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, awaitingCycle3DataState);

        sessionRepository.getStateController(expectedSessionId, AwaitingCycle3DataStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(awaitingCycle3DataState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, AwaitingCycle3DataState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(awaitingCycle3DataState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void cycle0And1MatchRequestSentStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final Cycle0And1MatchRequestSentState cycle0And1MatchRequestSentState = Cycle0And1MatchRequestSentStateBuilder.aCycle0And1MatchRequestSentState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, cycle0And1MatchRequestSentState);

        sessionRepository.getStateController(expectedSessionId, Cycle0And1MatchRequestSentStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(cycle0And1MatchRequestSentState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, Cycle0And1MatchRequestSentState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(cycle0And1MatchRequestSentState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void cycle3MatchRequestSentStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final Cycle3MatchRequestSentState cycle3MatchRequestSentState = Cycle3MatchRequestSentStateBuilder.aCycle3MatchRequestSentState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, cycle3MatchRequestSentState);

        sessionRepository.getStateController(expectedSessionId, Cycle3MatchRequestSentStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(cycle3MatchRequestSentState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, Cycle3MatchRequestSentState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(cycle3MatchRequestSentState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void successfulMatchStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final SuccessfulMatchState successfulMatchState = SuccessfulMatchStateBuilder.aSuccessfulMatchState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, successfulMatchState);

        sessionRepository.getStateController(expectedSessionId, SuccessfulMatchStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(successfulMatchState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, SuccessfulMatchState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(successfulMatchState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void userAccountCreatedStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final UserAccountCreatedState userAccountCreatedState = UserAccountCreatedStateBuilder.aUserAccountCreatedState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, userAccountCreatedState);

        sessionRepository.getStateController(expectedSessionId, UserAccountCreatedStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(userAccountCreatedState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, UserAccountCreatedState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(userAccountCreatedState), any(StateTransitionAction.class));
    }

    @Deprecated
    @Test
    public void userAccountCreationRequestSentStateIsCompatibleWithOldClass() {
        final SessionId expectedSessionId = aSessionId().build();
        final SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        final UserAccountCreationRequestSentState userAccountCreationRequestSentState = UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState().buildOld();

        final SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        dataStore.replace(sessionId, sessionStartedState, userAccountCreationRequestSentState);

        sessionRepository.getStateController(expectedSessionId, UserAccountCreationRequestSentStateTransitional.class);
        verify(controllerFactory, VerificationModeFactory.times(1)).build(eq(userAccountCreationRequestSentState), any(StateTransitionAction.class));

        sessionRepository.getStateController(expectedSessionId, UserAccountCreationRequestSentState.class);
        verify(controllerFactory, VerificationModeFactory.times(2)).build(eq(userAccountCreationRequestSentState), any(StateTransitionAction.class));
    }

    @Test(expected = InvalidSessionStateException.class)
    public void shouldThrowExceptionIfStateIsNotWhatIsExpected() {
        SessionId expectedSessionId = aSessionId().build();
        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        sessionRepository.getStateController(sessionId, IdpSelectedStateTransitional.class);
    }

    @Test
    public void createSession_shouldCreateAndStoreSession() throws Exception {
        SessionId expectedSessionId = aSessionId().build();
        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class);

        assertThat(sessionId).isEqualTo(expectedSessionId);
        assertThat(dataStore.containsKey(expectedSessionId)).isEqualTo(true);
        assertThat(sessionStartedMap.containsKey(expectedSessionId)).isEqualTo(true);
        verify(controllerFactory).build(eq(sessionStartedState), any(StateTransitionAction.class));
    }

    @Test
    public void stateTransitionAction_shouldUpdateDatastore() throws Exception {
        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class);
        verify(controllerFactory).build(eq(sessionStartedState), stateTransitionActionArgumentCaptor.capture());
        TestState state = new TestState();
        stateTransitionActionArgumentCaptor.getValue().transitionTo(state);

        assertThat(dataStore.get(sessionId)).isEqualTo((State) state);
    }

    @Test
    public void getState_shouldGetAnInterfaceImplementation() throws Exception {

        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class);
        verify(controllerFactory).build(eq(sessionStartedState), stateTransitionActionArgumentCaptor.capture());
        TestState state = new TestState();
        stateTransitionActionArgumentCaptor.getValue().transitionTo(state);

        sessionRepository.getStateController(sessionId, ResponsePreparedState.class);
        verify(controllerFactory).build(eq(state), any(StateTransitionAction.class));

    }

    @Test(expected = SessionTimeoutException.class)
    public void getState_shouldThrowTimeoutStateException_whenStateRequestedIsNotTimeoutStateAndTimeout() {

        DateTime now = DateTime.now();
        DateTimeFreezer.freezeTime(now);

        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        DateTimeFreezer.freezeTime(now.plusMinutes(3));
        sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class);
    }

    @Test(expected = SessionTimeoutException.class)
    public void getState_shouldThrowTimeoutStateException_whenStateRequestedIsNotTimeoutStateAndAlreadyTimeout() {

        DateTime now = DateTime.now();
        DateTimeFreezer.freezeTime(now);

        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        DateTimeFreezer.freezeTime(now.plusMinutes(3));
        try {
            sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class);

        } catch (Exception e) {

        }

        sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class); // it is set to timed out now
    }

    @Test
    public void getState_shouldNotThrowTimeoutStateException_whenRequestedAndActualStateIsErrorResponsePreparedStateAndSessionIsTimedout() {

        DateTime now = DateTime.now();
        DateTimeFreezer.freezeTime(now);

        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        DateTimeFreezer.freezeTime(now.plusMinutes(3));
        try {
            sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class);

        } catch (Exception e) {

        }

        sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class);
    }

    @Test
    public void getState_shouldReturnTimeoutController_whenTimeoutStateRequestedAndStateHasTimedOut() {
        DateTime now = DateTime.now();
        DateTimeFreezer.freezeTime(now);

        SessionStartedStateTransitional sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        DateTimeFreezer.freezeTime(now.plusMinutes(3));

        // this action will implicitly move the session state to TimedOut
        try {
            sessionRepository.getStateController(sessionId, SessionStartedStateTransitional.class);
        } catch (SessionTimeoutException e) {
        }

        sessionRepository.getStateController(sessionId, TimeoutState.class);

        verify(controllerFactory).build(timeoutStateArgumentCaptor.capture(), any(StateTransitionAction.class));

        TimeoutState timeoutState = timeoutStateArgumentCaptor.getValue();
        assertThat(timeoutState.getRequestId()).isEqualTo(sessionStartedState.getRequestId());
        assertThat(timeoutState.getRequestIssuerEntityId()).isEqualTo(sessionStartedState.getRequestIssuerEntityId());
        assertThat(timeoutState.getAssertionConsumerServiceUri()).isEqualTo(sessionStartedState.getAssertionConsumerServiceUri());

    }

    @Test
    public void getLevelOfAssuranceFromIdp(){
        SessionStartedStateTransitional state = aSessionStartedState().build();

        SessionId sessionId = sessionRepository.createSession(state);

        assertThat(sessionRepository.getLevelOfAssuranceFromIdp(sessionId)).isEqualTo(Optional.absent());
    }

    private class TestState extends AbstractState implements ResponsePreparedState {
        protected TestState() {
            super("smile", "requestIssuerId", defaultSessionExpiry, URI.create("/test-service-index"), aSessionId().build(), false);
        }

        @Override
        public Optional<String> getRelayState() {
            return absent();
        }
    }
}
