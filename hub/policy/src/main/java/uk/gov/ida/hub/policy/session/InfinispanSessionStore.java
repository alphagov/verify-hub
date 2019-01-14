package uk.gov.ida.hub.policy.session;

import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import java.util.concurrent.ConcurrentMap;

public class InfinispanSessionStore implements SessionStore {

    private final ConcurrentMap<SessionId, State> dataStore;

    public InfinispanSessionStore(ConcurrentMap<SessionId, State> dataStore) {
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
