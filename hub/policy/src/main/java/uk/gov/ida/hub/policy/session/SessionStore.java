package uk.gov.ida.hub.policy.session;

import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

public interface SessionStore {
    void insert(SessionId sessionId, State state);

    void replace(SessionId sessionId, State state);

    boolean hasSession(SessionId sessionId);

    State get(SessionId sessionId);
}
