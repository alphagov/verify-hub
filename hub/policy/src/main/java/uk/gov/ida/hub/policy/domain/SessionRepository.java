package uk.gov.ida.hub.policy.domain;

import com.codahale.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundException;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateException;
import uk.gov.ida.hub.policy.exception.SessionTimeoutException;
import uk.gov.ida.hub.policy.session.SessionStore;

import javax.inject.Inject;
import java.util.Optional;

import static java.text.MessageFormat.format;

public class SessionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRepository.class);

    private final SessionStore dataStore;
    private final StateControllerFactory controllerFactory;

    @Inject
    public SessionRepository(
            SessionStore dataStore,
            StateControllerFactory controllerFactory) {
        this.dataStore = dataStore;
        this.controllerFactory = controllerFactory;
    }

    public SessionId createSession(SessionStartedState startedState) {
        SessionId sessionId = startedState.getSessionId();

        dataStore.insert(sessionId, startedState);
        LOG.info(format("Session {0} created", sessionId.getSessionId()));

        return sessionId;
    }

    @Timed(name = Urls.SESSION_REPO_TIMED_GROUP)
    public <T extends State> StateController getStateController(
            final SessionId sessionId,
            final Class<T> expectedStateClass) {

        validateSessionExists(sessionId);

        State currentState = getCurrentState(sessionId);
        Class<? extends State> currentStateClass = currentState.getClass();

        handleTimeout(sessionId, currentState, currentStateClass, expectedStateClass);

        if (isAKindOf(expectedStateClass, currentStateClass) || currentStateClass.equals(TimeoutState.class)) {
            return controllerFactory.build(currentState, state -> dataStore.replace(sessionId, state));
        }

        throw new InvalidSessionStateException(sessionId, expectedStateClass, currentState.getClass());
    }

    @Timed(name = Urls.SESSION_REPO_TIMED_GROUP)
    public boolean sessionExists(SessionId sessionId) {
        return dataStore.hasSession(sessionId);
    }

    @Timed(name =Urls.SESSION_REPO_TIMED_GROUP)
    public Optional<LevelOfAssurance> getLevelOfAssuranceFromIdp(SessionId sessionId){

        State currentState = getCurrentState(sessionId);

        if(currentState instanceof Cycle0And1MatchRequestSentState){ // initial match request - no response received
            return Optional.of(((Cycle0And1MatchRequestSentState) currentState).getIdpLevelOfAssurance());
        }
        if(currentState instanceof SuccessfulMatchState){ // when no cycle 3 and no user account creation
            return Optional.of(((SuccessfulMatchState) currentState).getLevelOfAssurance());
        }
        if(currentState instanceof AwaitingCycle3DataState){ // when cycle3 on and user account creation on or off
            return Optional.of(((AwaitingCycle3DataState) currentState).getLevelOfAssurance());
        }
        if(currentState instanceof UserAccountCreatedState){ // when no cycle3 and user account creation
            return Optional.of(((UserAccountCreatedState) currentState).getLevelOfAssurance());
        }

        return Optional.empty();
    }

    private <T extends State> boolean isAKindOf(
            final Class<T> expectedStateClass,
            final Class<? extends State> currentStateClass) {

        return currentStateClass.equals(expectedStateClass) || expectedStateClass.isAssignableFrom(currentStateClass);
    }

    public boolean getTransactionSupportsEidas(SessionId sessionId) {
        return getCurrentState(sessionId).getTransactionSupportsEidas();
    }

    public String getRequestIssuerEntityId(SessionId sessionId) {
        return getCurrentState(sessionId).getRequestIssuerEntityId();
    }

    public boolean isSessionInState(SessionId sessionId, Class<? extends State> stateClass) {
        return stateClass.isAssignableFrom(getCurrentState(sessionId).getClass());
    }

    private State getCurrentState(SessionId sessionId) {
        return dataStore.get(sessionId);
    }

    public void validateSessionExists(SessionId sessionId) {
        if (!sessionExists(sessionId)) {
            throw new SessionNotFoundException(sessionId);
        }
    }

    private void handleTimeout(SessionId sessionId, State state, Class<? extends State> stateClass, Class<? extends State> expectedStateClass) {
        boolean needsStateChangedToTimeout = isTimedOut(sessionId) && !stateClass.equals(TimeoutState.class);
        if (needsStateChangedToTimeout) {
            TimeoutState timeoutState = new TimeoutState(
                    state.getRequestId(),
                    state.getRequestIssuerEntityId(),
                    state.getSessionExpiryTimestamp(),
                    state.getAssertionConsumerServiceUri(),
                    state.getSessionId(),
                    state.getTransactionSupportsEidas()
            );
            dataStore.replace(sessionId, timeoutState);
        }

        boolean unexpectedErrorState = isErrorState(stateClass) && !isErrorState(expectedStateClass);

        if (needsStateChangedToTimeout || unexpectedErrorState) {
            throw new SessionTimeoutException(format("Session {0} timed out.", sessionId.getSessionId()), sessionId, state.getRequestIssuerEntityId(), state.getSessionExpiryTimestamp(), state.getRequestId());
        }
    }

    private boolean isErrorState(Class clazz){
        return clazz.equals(ErrorResponsePreparedState.class)
                || clazz.equals(TimeoutState.class);
    }

    private boolean isTimedOut(SessionId sessionId) {
        if (dataStore.hasSession(sessionId)) {
            DateTime expiryTime = dataStore.get(sessionId).getSessionExpiryTimestamp();
            return DateTime.now().isAfter(expiryTime);
        } else {
            throw new SessionNotFoundException(sessionId);
        }
    }
}
