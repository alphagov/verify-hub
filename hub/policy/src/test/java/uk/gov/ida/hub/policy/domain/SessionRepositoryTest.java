package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.ResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateException;
import uk.gov.ida.hub.policy.exception.SessionTimeoutException;
import uk.gov.ida.hub.policy.session.InfinispanSessionStore;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Optional.absent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setup() {
        dataStore = new ConcurrentHashMap<>();
        sessionRepository = new SessionRepository(new InfinispanSessionStore(dataStore), controllerFactory);
    }

    @Test(expected = InvalidSessionStateException.class)
    public void shouldThrowExceptionIfStateIsNotWhatIsExpected() {
        SessionId expectedSessionId = aSessionId().build();
        SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(defaultSessionExpiry).withSessionId(expectedSessionId).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        sessionRepository.getStateController(sessionId, IdpSelectedState.class);
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

    @Test(expected = SessionTimeoutException.class)
    public void getState_shouldThrowTimeoutStateException_whenStateRequestedIsNotTimeoutStateAndTimeout() {

        DateTime now = DateTime.now();
        DateTimeFreezer.freezeTime(now);

        SessionStartedState sessionStartedState = aSessionStartedState().withSessionExpiryTimestamp(now).build();
        SessionId sessionId = sessionRepository.createSession(sessionStartedState);

        DateTimeFreezer.freezeTime(now.plusMinutes(3));
        sessionRepository.getStateController(sessionId, SessionStartedState.class);
    }

    @Test(expected = SessionTimeoutException.class)
    public void getState_shouldThrowTimeoutStateException_whenStateRequestedIsNotTimeoutStateAndAlreadyTimeout() {

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

        assertThat(sessionRepository.getLevelOfAssuranceFromIdp(sessionId)).isEqualTo(Optional.absent());
    }

    private class TestState extends AbstractState implements ResponsePreparedState {
        protected TestState() {
            super("smile", "requestIssuerId", defaultSessionExpiry, URI.create("/test-service-index"), aSessionId().build(), false, false);
        }

        @Override
        public Optional<String> getRelayState() {
            return absent();
        }
    }
}
