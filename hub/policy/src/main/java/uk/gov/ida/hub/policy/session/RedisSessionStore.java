package uk.gov.ida.hub.policy.session;

import org.redisson.api.RMap;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import java.util.concurrent.TimeUnit;

public class RedisSessionStore implements SessionStore {
    private final RMap<SessionId, State> dataStore;
    private final Long expiryTimeInMinutes;

    public RedisSessionStore(RMap<SessionId, State> dataStore, Long expiryTimeInMinutes) {
        this.dataStore = dataStore;
        this.expiryTimeInMinutes = expiryTimeInMinutes;
    }

    @Override
    public void insert(SessionId sessionId, State value) {
        dataStore.put(sessionId, value);
        dataStore.expire(expiryTimeInMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void replace(SessionId sessionId, State value) {
        dataStore.replace(sessionId, value);
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
