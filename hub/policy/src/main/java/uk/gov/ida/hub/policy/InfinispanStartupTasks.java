package uk.gov.ida.hub.policy;

import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class InfinispanStartupTasks implements Managed {

    private final ConcurrentMap<SessionId, State> sessionStateStore;

    @Inject
    public InfinispanStartupTasks(ConcurrentMap<SessionId, State> sessionStateStore) {
        this.sessionStateStore = sessionStateStore;
    }

    @Override
    public void start() throws Exception {
        SessionId newSessionId = SessionId.createNewSessionId();
        sessionStateStore.get(newSessionId);
    }

    @Override
    public void stop() throws Exception {
    }
}
