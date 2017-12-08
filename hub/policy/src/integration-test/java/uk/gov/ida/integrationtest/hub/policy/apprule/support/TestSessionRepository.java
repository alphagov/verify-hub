package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class TestSessionRepository {

    private final ConcurrentMap<SessionId, State> dataStore;
    private final ConcurrentMap<SessionId, DateTime> sessionStartedMap;

    @Inject
    public TestSessionRepository(ConcurrentMap<SessionId, State> dataStore, ConcurrentMap<SessionId, DateTime> sessionStartedMap) {
        this.dataStore = dataStore;
        this.sessionStartedMap = sessionStartedMap;
    }

    public void createSession(SessionId sessionId, State state) {
        dataStore.put(sessionId, state);
        sessionStartedMap.put(sessionId, state.getSessionExpiryTimestamp());
    }

    public State getSession(SessionId sessionId) {
        return dataStore.get(sessionId);
    }
}
