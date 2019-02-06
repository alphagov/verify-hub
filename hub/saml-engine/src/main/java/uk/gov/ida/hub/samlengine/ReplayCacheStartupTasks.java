package uk.gov.ida.hub.samlengine;

import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKey;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKeyForInitilization;
import uk.gov.ida.saml.hub.validators.authnrequest.IdExpirationCache;

import javax.inject.Inject;

public class ReplayCacheStartupTasks implements Managed {

    private IdExpirationCache<String> assertionIdReplayCache;
    private IdExpirationCache<AuthnRequestIdKey> authnRequestIdReplayCache;

    @Inject
    public ReplayCacheStartupTasks(IdExpirationCache<String> assertionIdReplayCache, IdExpirationCache<AuthnRequestIdKey> authnRequestIdReplayCache) {
        this.assertionIdReplayCache = assertionIdReplayCache;
        this.authnRequestIdReplayCache = authnRequestIdReplayCache;
    }

    @Override
    public void start() throws Exception {
        assertionIdReplayCache.getExpiration("some-string");
        authnRequestIdReplayCache.getExpiration(new AuthnRequestIdKeyForInitilization("some-id"));
    }

    @Override
    public void stop() throws Exception {
    }
}
