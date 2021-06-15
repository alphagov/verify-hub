package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import certificates.values.CACertificates;
import httpstub.HttpStubRule;
import io.dropwizard.Application;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.opensaml.core.config.InitializationService;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.Constants;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlEngineAppExtension extends TestDropwizardAppExtension {
    public static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    public static final int REDIS_PORT = 6383;
    public static RedisTestRule redis = new RedisTestRule(REDIS_PORT);

    public static SamlEngineBuilder forApp(final Class<? extends Application> app) {
        return new SamlEngineBuilder(app);
    }

    public static class SamlEngineBuilder extends TestDropwizardAppExtension.Builder {
        public SamlEngineBuilder(Class<? extends Application> app) {
            super(app);
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
        }

        public SamlEngineBuilder withDefaultConfigOverridesAnd(String... extraOverrides) {
            String[] defaultOverrides = {
                    "saml.entityId: " + HUB_ENTITY_ID,
                    "saml.expectedDestination: http://localhost",
                    "server.applicationConnectors[0].port: 0",
                    "server.adminConnectors[0].portL: 0",
                    "privateSigningKeyConfiguration.key: " + HUB_TEST_PRIVATE_SIGNING_KEY,
                    "privateSigningKeyConfiguration.type: encoded",
                    "primaryPrivateEncryptionKeyConfiguration.key: " + HUB_TEST_PRIVATE_ENCRYPTION_KEY,
                    "primaryPrivateEncryptionKeyConfiguration.type: encoded",
                    "secondaryPrivateEncryptionKeyConfiguration.key: " + TEST_PRIVATE_KEY,
                    "secondaryPrivateEncryptionKeyConfiguration.type: encoded",
                    "rpTrustStoreConfiguration.path: " + rpTrustStore.getAbsolutePath(),
                    "rpTrustStoreConfiguration.password: " + rpTrustStore.getPassword(),
                    "metadata.trustStore.path: " + metadataTrustStore.getAbsolutePath(),
                    "metadata.trustStore.password: " + metadataTrustStore.getPassword(),
                    "metadata.uri: " + "http://localhost:" + verifyMetadataServer.getPort() + VERIFY_METADATA_PATH,
                    "metadata.hubTrustStore.path: " + hubTrustStore.getAbsolutePath(),
                    "metadata.hubTrustStore.password: " + hubTrustStore.getPassword(),
                    "metadata.idpTrustStore.path: " + idpTrustStore.getAbsolutePath(),
                    "metadata.idpTrustStore.password: " + idpTrustStore.getPassword(),
                    "certificatesConfigCacheExpiry: " + "20s",
                    "redis.uri: " + "redis://localhost:" + REDIS_PORT
            };

            this.configOverrides(ArrayUtils.addAll(defaultOverrides, extraOverrides));
            return this;
        }
    }

    public static void tearDown() {
        redis.after();
        metadataTrustStore.delete();
        hubTrustStore.delete();
        idpTrustStore.delete();
        rpTrustStore.delete();
    }
}
