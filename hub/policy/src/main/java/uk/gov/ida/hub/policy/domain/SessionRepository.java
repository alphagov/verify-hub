package uk.gov.ida.hub.policy.domain;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundException;
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
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingStateTransitional;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchStateTransitional;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.TransitionalStateConverter;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentStateTransitional;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateException;
import uk.gov.ida.hub.policy.exception.SessionTimeoutException;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

import static java.text.MessageFormat.format;

public class SessionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRepository.class);

    private final ConcurrentMap<SessionId, State> dataStore;
    private final ConcurrentMap<SessionId, DateTime> sessionStartedMap;
    private final StateControllerFactory controllerFactory;

    @Inject
    public SessionRepository(
            ConcurrentMap<SessionId, State> dataStore,
            ConcurrentMap<SessionId, DateTime> sessionStartedMap,
            StateControllerFactory controllerFactory) {

        this.dataStore = dataStore;
        this.sessionStartedMap = sessionStartedMap;
        this.controllerFactory = controllerFactory;
    }

    public SessionId createSession(SessionStartedState startedState) {
        SessionId sessionId = startedState.getSessionId();

        dataStore.put(sessionId, TransitionalStateConverter.convertToTransitional(startedState));
        sessionStartedMap.put(sessionId, startedState.getSessionExpiryTimestamp());
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
            return controllerFactory.build(currentState, state -> dataStore.replace(sessionId, TransitionalStateConverter.convertToTransitional(state)));
        }

        throw new InvalidSessionStateException(sessionId, expectedStateClass, currentState.getClass());
    }

    @Timed(name = Urls.SESSION_REPO_TIMED_GROUP)
    public boolean sessionExists(SessionId sessionId) {
        return dataStore.containsKey(sessionId);
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
        return Optional.absent();
    }

    private <T extends State> boolean isAKindOf(
            final Class<T> expectedStateClass,
            final Class<? extends State> currentStateClass) {

        return currentStateClass.equals(expectedStateClass) || expectedStateClass.isAssignableFrom(currentStateClass)
                || isATransitionalKindOf(expectedStateClass, currentStateClass);
    }

    @Deprecated
    private <T extends State> boolean isATransitionalKindOf(
            final Class<T> expectedStateClass,
            final Class<? extends State> currentStateClass) {

        if (expectedStateClass.equals(IdpSelectingState.class)) {
            return IdpSelectingStateTransitional.class.isAssignableFrom(currentStateClass);
        }

        if (currentStateClass.equals(IdpSelectedStateTransitional.class) && expectedStateClass.equals(IdpSelectedState.class)) {
            return true;
        }

        if (currentStateClass.equals(SessionStartedStateTransitional.class) && expectedStateClass.equals(SessionStartedState.class)) {
            return true;
        }

        if (currentStateClass.equals(AuthnFailedErrorStateTransitional.class) && expectedStateClass.equals(AuthnFailedErrorState.class)) {
            return true;
        }

        if (currentStateClass.equals(FraudEventDetectedStateTransitional.class) && expectedStateClass.equals(FraudEventDetectedState.class)) {
            return true;
        }

        if (currentStateClass.equals(RequesterErrorStateTransitional.class) && expectedStateClass.equals(RequesterErrorState.class)) {
            return true;
        }

        if (currentStateClass.equals(AwaitingCycle3DataStateTransitional.class) && expectedStateClass.equals(AwaitingCycle3DataState.class)) {
            return true;
        }

        if (currentStateClass.equals(Cycle0And1MatchRequestSentStateTransitional.class) && expectedStateClass.equals(Cycle0And1MatchRequestSentState.class)) {
            return true;
        }

        if (currentStateClass.equals(Cycle3MatchRequestSentStateTransitional.class) && expectedStateClass.equals(Cycle3MatchRequestSentState.class)) {
            return true;
        }

        if (currentStateClass.equals(SuccessfulMatchStateTransitional.class) && expectedStateClass.equals(SuccessfulMatchState.class)) {
            return true;
        }

        if (currentStateClass.equals(UserAccountCreatedStateTransitional.class) && expectedStateClass.equals(UserAccountCreatedState.class)) {
            return true;
        }

        if (currentStateClass.equals(UserAccountCreationRequestSentStateTransitional.class) && expectedStateClass.equals(UserAccountCreationRequestSentState.class)) {
            return true;
        }

        return false;
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
            dataStore.replace(
                    sessionId,
                    new TimeoutState(
                            state.getRequestId(),
                            state.getRequestIssuerEntityId(),
                            state.getSessionExpiryTimestamp(),
                            state.getAssertionConsumerServiceUri(),
                            state.getSessionId(),
                            state.getTransactionSupportsEidas()
                    ));
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
        if (sessionStartedMap.containsKey(sessionId)) {
            DateTime expiryTime = sessionStartedMap.get(sessionId);
            return DateTime.now().isAfter(expiryTime);
        } else {
            throw new SessionNotFoundException(sessionId);
        }
    }
}
