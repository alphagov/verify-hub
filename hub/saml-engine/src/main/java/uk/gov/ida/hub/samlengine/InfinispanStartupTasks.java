package uk.gov.ida.hub.samlengine;

import io.dropwizard.lifecycle.Managed;
import org.joda.time.DateTime;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKey;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKeyForInitilization;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;

public class InfinispanStartupTasks implements Managed {

    private ConcurrentMap<String, DateTime> assertionIdReplayCache;
    private ConcurrentMap<AuthnRequestIdKey, DateTime> authnRequestIdReplayCache;

    @Inject
    public InfinispanStartupTasks(ConcurrentMap<String, DateTime> assertionIdReplayCache, ConcurrentMap<AuthnRequestIdKey, DateTime> authnRequestIdReplayCache) {
        this.assertionIdReplayCache = assertionIdReplayCache;
        this.authnRequestIdReplayCache = authnRequestIdReplayCache;
    }

    @Override
    public void start() throws Exception {
        assertionIdReplayCache.get("some-string");
        authnRequestIdReplayCache.get(new AuthnRequestIdKeyForInitilization("some-id"));
    }

    @Override
    public void stop() throws Exception {
    }
}
