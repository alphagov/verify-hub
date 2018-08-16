package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class TestSessionRepository {

    private final ConcurrentMap<SessionId, State> dataStore;

    @Inject
    public TestSessionRepository(ConcurrentMap<SessionId, State> dataStore) {
        this.dataStore = dataStore;
    }

    public void createSession(SessionId sessionId, State state) {
        dataStore.put(sessionId, state);
    }

    public State getSession(SessionId sessionId) {
        return dataStore.get(sessionId);
    }
}
