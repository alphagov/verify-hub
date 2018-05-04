package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.google.common.collect.ImmutableList;
import helpers.ResourceHelpers;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.concurrent.ConcurrentMap;

public class PolicyAppRule extends DropwizardAppRule<PolicyConfiguration> {

    public PolicyAppRule(final ConfigOverride... configOverrides) {
        super(PolicyIntegrationApplication.class, ResourceHelpers.resourceFilePath("policy.yml"), withDefaultOverrides(configOverrides));
    }

    public static ConfigOverride[] withDefaultOverrides(final ConfigOverride... configOverrides) {
        ImmutableList<ConfigOverride> mergedConfigOverrides = ImmutableList.<ConfigOverride>builder()
                .add(configOverrides)
                .build();
        return mergedConfigOverrides.toArray(new ConfigOverride[mergedConfigOverrides.size()]);
    }

    @Override
    protected void before() {
        super.before();
    }

    @Override
    protected void after() {
        super.after();
    }

    public URI uri(String path) {
        return UriBuilder.fromUri("http://localhost")
                .path(path)
                .port(getLocalPort())
                .build();
    }

    public ConcurrentMap<SessionId, State> getDataStore() {
       return ((PolicyIntegrationApplication)this.getApplication()).getDataStore();
    }

    public <T extends State> T getSessionState(SessionId sessionId, Class<T> stateClazz) {
        return stateClazz.cast(getDataStore().get(sessionId));
    }
}
