package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.session.SessionStore;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class TestSessionRepository {

    private final SessionStore dataStore;

    @Inject
    public TestSessionRepository(SessionStore dataStore) {
        this.dataStore = dataStore;
    }

    public void createSession(SessionId sessionId, State state) {
        dataStore.insert(sessionId, state);
    }

    public State getSession(SessionId sessionId) {
        return dataStore.get(sessionId);
    }
}
