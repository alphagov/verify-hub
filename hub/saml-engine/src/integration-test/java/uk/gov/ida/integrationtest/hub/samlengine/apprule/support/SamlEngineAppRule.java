package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import certificates.values.CACertificates;
import com.squarespace.jersey2.guice.BootstrapUtils;
import httpstub.HttpStubRule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.Constants;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.hub.samlengine.SamlEngineConfiguration;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.dropwizard.testing.ConfigOverride.config;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlEngineAppRule extends DropwizardAppRule<SamlEngineConfiguration> {
    public static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    public static final int REDIS_PORT = 6383;

    public RedisTestRule redis = new RedisTestRule(REDIS_PORT);

    public SamlEngineAppRule(ConfigOverride... configOverrides) {
        super(SamlEngineApplication.class,
                ResourceHelpers.resourceFilePath("saml-engine.yml"),
                withDefaultOverrides(null, null, configOverrides)
        );
        BootstrapUtils.reset();
    }

    public SamlEngineAppRule(String proxyHost, String proxyPort, ConfigOverride... configOverrides) {
        super(SamlEngineApplication.class,
                ResourceHelpers.resourceFilePath("saml-engine.yml"),
                withDefaultOverrides(proxyHost, proxyPort, configOverrides)
        );
        BootstrapUtils.reset();
    }

    public static ConfigOverride[] withDefaultOverrides(String proxyHost, String proxyPort, ConfigOverride... configOverrides) {
        if (proxyHost != null && proxyPort != null) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }
        List<ConfigOverride> overrides = Stream.of(
                config("saml.entityId", HUB_ENTITY_ID),
                config("saml.expectedDestination", "http://localhost"),
                config("server.applicationConnectors[0].port", "0"),
                config("server.adminConnectors[0].port", "0"),
                config("privateSigningKeyConfiguration.key", HUB_TEST_PRIVATE_SIGNING_KEY),
                config("privateSigningKeyConfiguration.type", "encoded"),
                config("primaryPrivateEncryptionKeyConfiguration.key", HUB_TEST_PRIVATE_ENCRYPTION_KEY),
                config("primaryPrivateEncryptionKeyConfiguration.type", "encoded"),
                config("secondaryPrivateEncryptionKeyConfiguration.key", TEST_PRIVATE_KEY),
                config("secondaryPrivateEncryptionKeyConfiguration.type", "encoded"),
                config("rpTrustStoreConfiguration.path", rpTrustStore.getAbsolutePath()),
                config("rpTrustStoreConfiguration.password", rpTrustStore.getPassword()),
                config("metadata.trustStore.path", metadataTrustStore.getAbsolutePath()),
                config("metadata.trustStore.password", metadataTrustStore.getPassword()),
                config("metadata.uri", "http://localhost:" + verifyMetadataServer.getPort() + VERIFY_METADATA_PATH),
                config("metadata.hubTrustStore.path", hubTrustStore.getAbsolutePath()),
                config("metadata.hubTrustStore.password", hubTrustStore.getPassword()),
                config("metadata.idpTrustStore.path", idpTrustStore.getAbsolutePath()),
                config("metadata.idpTrustStore.password", idpTrustStore.getPassword()),
                config("certificatesConfigCacheExpiry", "20s"),
                config("redis.uri", "redis://localhost:" + REDIS_PORT)
        ).collect(Collectors.toList());
        overrides.addAll(Arrays.asList(configOverrides));
        return overrides.toArray(new ConfigOverride[0]);
    }

    @Override
    protected void before() {
        try {
            redis.before();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        metadataTrustStore.create();
        hubTrustStore.create();
        idpTrustStore.create();
        rpTrustStore.create();
        CollectorRegistry.defaultRegistry.clear();

        try {
            InitializationService.initialize();

            verifyMetadataServer.reset();
            verifyMetadataServer.register(VERIFY_METADATA_PATH, 200, Constants.APPLICATION_SAMLMETADATA_XML, new MetadataFactory().defaultMetadata());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        super.before();
    }

    @Override
    protected void after() {
        redis.after();
        metadataTrustStore.delete();
        hubTrustStore.delete();
        idpTrustStore.delete();
        rpTrustStore.delete();

        super.after();
    }

    public URI getUri(String path) {
        return UriBuilder.fromUri("http://localhost")
                .path(path)
                .port(getLocalPort())
                .build();
    }

}
