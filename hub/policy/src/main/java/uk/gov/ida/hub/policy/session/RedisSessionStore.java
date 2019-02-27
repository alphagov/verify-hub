package uk.gov.ida.hub.policy.session;

import io.lettuce.core.api.sync.RedisCommands;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

public class RedisSessionStore implements SessionStore {
    private final RedisCommands<SessionId, State> dataStore;
    private final Long recordTTL;

    public RedisSessionStore(RedisCommands<SessionId, State> dataStore, Long recordTTL) {
        this.dataStore = dataStore;
        this.recordTTL = recordTTL;
    }

    @Override
    public void insert(SessionId sessionId, State value) {
        dataStore.setex(sessionId, recordTTL, value);
    }

    @Override
    public void replace(SessionId sessionId, State value) {
        Long ttl = dataStore.ttl(sessionId);
        dataStore.setex(sessionId, ttl, value);
    }

    @Override
    public boolean hasSession(SessionId sessionId) {
        return dataStore.exists(sessionId) > 0;
    }

    @Override
    public State get(SessionId sessionId) {
        return dataStore.get(sessionId);
    }
}
