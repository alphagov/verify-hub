package uk.gov.ida.hub.policy;

import io.dropwizard.lifecycle.Managed;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class InfinispanStartupTasks implements Managed {

    private final ConcurrentMap<SessionId, State> sessionCache;
    private final ConcurrentMap<SessionId, DateTime> expirationCache;

    @Inject
    public InfinispanStartupTasks(ConcurrentMap<SessionId, State> sessionCache, ConcurrentMap<SessionId, DateTime> expirationCache) {
        this.sessionCache = sessionCache;
        this.expirationCache = expirationCache;
    }

    @Override
    public void start() throws Exception {
        SessionId newSessionId = SessionId.createNewSessionId();
        sessionCache.get(newSessionId);
        expirationCache.get(newSessionId);
    }

    @Override
    public void stop() throws Exception {
    }
}
