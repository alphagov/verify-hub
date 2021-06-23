package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import certificates.values.CACertificates;
import httpstub.HttpStubRule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.Constants;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.*;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlEngineAppExtension extends DropwizardAppExtension  {
    public static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    public static final int REDIS_PORT = 6383;
    public static RedisTestRule redis = new RedisTestRule(REDIS_PORT);

    public SamlEngineAppExtension(Class applicationClass, @Nullable String configPath, ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
    }

    public SamlEngineClient getClient() {
        return new SamlEngineClient();
    }

    public void tearDown() {
        redis.after();
        metadataTrustStore.delete();
        hubTrustStore.delete();
        idpTrustStore.delete();
        rpTrustStore.delete();
    }

    public static class SamlEngineAppExtensionBuilder {
        private ConfigOverride[] configOverrides = new ConfigOverride[]{};

        public SamlEngineAppExtensionBuilder withConfigOverrides(ConfigOverride... overrides) {
            configOverrides = overrides;
            return this;
        }

        public SamlEngineAppExtension build() {
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

            return new SamlEngineAppExtension(
                    SamlEngineApplication.class,
                    ResourceHelpers.resourceFilePath("saml-engine.yml"),
                    ArrayUtils.addAll(configOverrides, defaultConfigOverrides())
            );
        }

        private static ConfigOverride[] defaultConfigOverrides() {
            return new ConfigOverride[]{
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
                    config("redis.uri", "redis://localhost:" + REDIS_PORT),
            };
        }
    }

    public class SamlEngineClient {
        private Client client;

        public SamlEngineClient() { client = client(); }

        public Response postTargetMain(URI uri, Object entity) { return postTarget(uri, getLocalPort(), entity); };

        public Response postTargetMain(String path, Object entity) { return postTargetMain(UriBuilder.fromPath(path).build(), entity); };

        public Response postTargetAdmin(String path, Object entity) { return postTarget(UriBuilder.fromPath(path).build(), getAdminPort(), entity); };

        public Response postTarget(URI uri, int port, Object entity) {
            UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost").port(port).path(uri.getRawPath());
            if (uri.getQuery() != null) {
                uriBuilder.replaceQuery(uri.getQuery());
            }

            return client
                    .target(uriBuilder.build())
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        }
    }
}
