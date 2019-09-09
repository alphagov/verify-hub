package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import certificates.values.CACertificates;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Arrays.asList;

public class PolicyAppRule extends DropwizardAppRule<PolicyConfiguration> {

    private static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();

    public PolicyAppRule(final ConfigOverride... configOverrides) {
        super(PolicyIntegrationApplication.class, ResourceHelpers.resourceFilePath("policy.yml"), withDefaultOverrides(configOverrides));
    }

    private static ConfigOverride[] withDefaultOverrides(final ConfigOverride... configOverrides) {
        List<ConfigOverride> overrides = new ArrayList<>(List.of(
                config("clientTrustStoreConfiguration.path", clientTrustStore.getAbsolutePath()),
                config("clientTrustStoreConfiguration.password", clientTrustStore.getPassword()),
                config("eventEmitterConfiguration.enabled", "false")));

        if (configOverrides != null) {
            overrides.addAll(asList(configOverrides));
        }
        return overrides.toArray(new ConfigOverride[0]);
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
