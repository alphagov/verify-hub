package uk.gov.ida.hub.policy;

import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.session.SessionStore;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class SessionStoreStartupTasks implements Managed {

    private final SessionStore sessionStore;

    @Inject
    public SessionStoreStartupTasks(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void start() throws Exception {
        SessionId newSessionId = SessionId.createNewSessionId();
        sessionStore.get(newSessionId);
    }

    @Override
    public void stop() throws Exception {
    }
}
