package uk.gov.ida.hub.policy.domain;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.ResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateException;
import uk.gov.ida.hub.policy.exception.SessionTimeoutException;
import uk.gov.ida.hub.policy.session.SessionStore;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;

@ExtendWith(MockitoExtension.class)
public class SessionRepositoryTest {

    private SessionRepository sessionRepository;
    private ConcurrentMap<SessionId, State> dataStore;
    private DateTime defaultSessionExpiry = DateTime.now().plusDays(8);

    @Mock
    private StateControllerFactory controllerFactory;

    @Captor
    private ArgumentCaptor<StateTransitionAction> stateTransitionActionArgumentCaptor = null;

    @Captor
    private ArgumentCaptor<TimeoutState> timeoutStateArgumentCaptor = null;

    @BeforeEach
    public void setup() {
        dataStore = new ConcurrentHashMap<>();
        sessionRepository = new SessionRepository(new ConcurrentMapSessionStore(dataStore), controllerFactory);
    }

    @Test
    public void shouldThrowExceptionIfStateIsNotWhatIsExpected() {
        Assertions.assertThrows(InvalidSessionStateException.class, () -> {
            SessionId expectedSessionId = aSessionId().build();
            SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
            SessionId sessionId = sessionRepository.createSession(sessionStartedState);

            sessionRepository.getStateController(sessionId, IdpSelectedState.class);
        });
    }

    @Test
    public void createSession_shouldCreateAndStoreSession() {
        SessionId expectedSessionId = aSessionId().build();
        SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        sessionRepository.getStateController(sessionId, SessionStartedState.class);

        assertThat(sessionId).isEqualTo(expectedSessionId);
        assertThat(dataStore.containsKey(expectedSessionId)).isEqualTo(true);
        verify(controllerFactory).build(eq(sessionStartedState), any(StateTransitionAction.class));
    }

    @Test
    public void stateTransitionAction_shouldUpdateDatastore() {
        SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        sessionRepository.getStateController(sessionId, SessionStartedState.class);
        verify(controllerFactory).build(eq(sessionStartedState), stateTransitionActionArgumentCaptor.capture());
        TestState state = new TestState();
        stateTransitionActionArgumentCaptor.getValue().transitionTo(state);

        assertThat(dataStore.get(sessionId)).isEqualTo(state);
    }

    @Test
    public void getState_shouldGetAnInterfaceImplementation() {

        SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);
        sessionRepository.getStateController(sessionId, SessionStartedState.class);
        verify(controllerFactory).build(eq(sessionStartedState), stateTransitionActionArgumentCaptor.capture());
        TestState state = new TestState();
        stateTransitionActionArgumentCaptor.getValue().transitionTo(state);

        sessionRepository.getStateController(sessionId, ResponsePreparedState.class);
        verify(controllerFactory).build(eq(state), any(StateTransitionAction.class));
    }

    @Test
    public void getState_shouldThrowTimeoutStateException_whenStateRequestedIsNotTimeoutStateAndTimeout() {
        Assertions.assertThrows(SessionTimeoutException.class, () -> {
            DateTime now = DateTime.now();
            DateTimeFreezer.freezeTime(now);

            SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
            SessionId sessionId = sessionRepository.createSession(sessionStartedState);

            DateTimeFreezer.freezeTime(now.plusMinutes(3));
            sessionRepository.getStateController(sessionId, SessionStartedState.class);
        });
    }

    @Test
    public void getState_shouldThrowTimeoutStateException_whenStateRequestedIsNotTimeoutStateAndAlreadyTimeout() {
        Assertions.assertThrows(SessionTimeoutException.class, () -> {
            DateTime now = DateTime.now();
            DateTimeFreezer.freezeTime(now);

            SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
            SessionId sessionId = sessionRepository.createSession(sessionStartedState);

            DateTimeFreezer.freezeTime(now.plusMinutes(3));
            try {
                sessionRepository.getStateController(sessionId, SessionStartedState.class);

            } catch (Exception e) {

            }

            sessionRepository.getStateController(sessionId, SessionStartedState.class); // it is set to timed out now
        });
    }

    @Test
    public void getState_shouldNotThrowTimeoutStateException_whenRequestedAndActualStateIsErrorResponsePreparedStateAndSessionIsTimedout() {

        DateTime now = DateTime.now();
        DateTimeFreezer.freezeTime(now);

        SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        DateTimeFreezer.freezeTime(now.plusMinutes(3));
        try {
            sessionRepository.getStateController(sessionId, SessionStartedState.class);

        } catch (Exception e) {

        }

        sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class);
    }

    @Test
    public void getState_shouldReturnTimeoutController_whenTimeoutStateRequestedAndStateHasTimedOut() {
        DateTime now = DateTime.now();
        DateTimeFreezer.freezeTime(now);

        SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        DateTimeFreezer.freezeTime(now.plusMinutes(3));

        // this action will implicitly move the session state to TimedOut
        try {
            sessionRepository.getStateController(sessionId, SessionStartedState.class);
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
        SessionStartedState state = aSessionStartedState().build();

        SessionId sessionId = sessionRepository.createSession(state);

        assertThat(sessionRepository.getLevelOfAssuranceFromIdp(sessionId)).isEqualTo(Optional.empty());
    }

    private class TestState extends AbstractState implements ResponsePreparedState {
        protected TestState() {
            super("smile", "requestIssuerId", defaultSessionExpiry, URI.create("/test-service-index"), aSessionId().build(), false);
        }

        @Override
        public Optional<String> getRelayState() {
            return Optional.empty();
        }
    }

    private static class ConcurrentMapSessionStore implements SessionStore {

        private final ConcurrentMap<SessionId, State> dataStore;

        public ConcurrentMapSessionStore(ConcurrentMap<SessionId, State> dataStore) {
            this.dataStore = dataStore;
        }

        @Override
        public void insert(SessionId sessionId, State state) {
            dataStore.put(sessionId, state);
        }

        @Override
        public void replace(SessionId sessionId, State state) {
            dataStore.replace(sessionId, state);
        }

        @Override
        public boolean hasSession(SessionId sessionId) {
            return dataStore.containsKey(sessionId);
        }

        @Override
        public State get(SessionId sessionId) {
            return dataStore.get(sessionId);
        }
    }
}
