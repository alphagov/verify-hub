package uk.gov.ida.hub.policy;

import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class InfinispanStartupTasks implements Managed {

    private final ConcurrentMap<SessionId, State> sessionCache;

    @Inject
    public InfinispanStartupTasks(ConcurrentMap<SessionId, State> sessionCache) {
        this.sessionCache = sessionCache;
    }

    @Override
    public void start() throws Exception {
        SessionId newSessionId = SessionId.createNewSessionId();
        sessionCache.get(newSessionId);
    }

    @Override
    public void stop() throws Exception {
    }
}
