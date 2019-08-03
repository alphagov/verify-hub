package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import certificates.values.CACertificates;
import com.google.common.collect.ImmutableList;
import helpers.ResourceHelpers;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.concurrent.ConcurrentMap;

import static io.dropwizard.testing.ConfigOverride.config;

public class PolicyAppRule extends DropwizardAppRule<PolicyConfiguration> {

    private static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();

    public PolicyAppRule(final ConfigOverride... configOverrides) {
        super(PolicyIntegrationApplication.class, ResourceHelpers.resourceFilePath("policy.yml"), withDefaultOverrides(configOverrides));
    }

    public static ConfigOverride[] withDefaultOverrides(final ConfigOverride... configOverrides) {
        ImmutableList<ConfigOverride> mergedConfigOverrides = ImmutableList.<ConfigOverride>builder()
                .add(config("clientTrustStoreConfiguration.path", clientTrustStore.getAbsolutePath()))
                .add(config("clientTrustStoreConfiguration.password", clientTrustStore.getPassword()))
                .add(config("eventEmitterConfiguration.enabled", "false"))
                .add(configOverrides)
                .build();
        return mergedConfigOverrides.toArray(new ConfigOverride[0]);
    }

    @Override
    protected void before() {
        clientTrustStore.create();

        super.before();
    }

    @Override
    protected void after() {
        clientTrustStore.delete();

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
