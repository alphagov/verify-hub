package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

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
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlProxyAppExtension extends DropwizardAppExtension {
    private static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();


    public SamlProxyAppExtension(Class applicationClass, @Nullable String configPath, ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
    }

    public static void tearDown() {
        metadataTrustStore.delete();
        hubTrustStore.delete();
        idpTrustStore.delete();
        rpTrustStore.delete();
    }

    public static SamlProxyAppExtensionBuilder builder() { return new SamlProxyAppExtensionBuilder(); }

    public SamlProxyClient getClient() {
        return new SamlProxyClient();
    }

    public static class SamlProxyAppExtensionBuilder {
        private ConfigOverride[] configOverrides = new ConfigOverride[]{};

        public SamlProxyAppExtensionBuilder withConfigOverrides(ConfigOverride... overrides) {
            configOverrides = overrides;
            return this;
        }

        public SamlProxyAppExtension build() {
            metadataTrustStore.create();
            hubTrustStore.create();
            idpTrustStore.create();
            rpTrustStore.create();
            CollectorRegistry.defaultRegistry.clear();

            try {
                InitializationService.initialize();
                verifyMetadataServer.reset();
                verifyMetadataServer.register(
                        VERIFY_METADATA_PATH,
                        200,
                        Constants.APPLICATION_SAMLMETADATA_XML,
                        new MetadataFactory().defaultMetadata()
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return new SamlProxyAppExtension(
                    SamlProxyApplication.class,
                    ResourceHelpers.resourceFilePath("saml-proxy.yml"),
                    ArrayUtils.addAll(configOverrides, defaultConfigOverrides())
            );
        }

        private static ConfigOverride[] defaultConfigOverrides() {
            return new ConfigOverride[]{
                    config("saml.entityId", HUB_ENTITY_ID),
                    config("server.applicationConnectors[0].port", "0"),
                    config("server.adminConnectors[0].port", "0"),
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
                    config("eventEmitterConfiguration.enabled", "false"),
            };
        }
    }

    public class SamlProxyClient {
        private Client client;

        public SamlProxyClient() { client = client(); }

        public Response getTargetMain(URI uri) { return getTarget(uri, getLocalPort()); }

        public <T> T getTargetMain(URI uri, Class<T> responseClass) { return getTarget(uri, getLocalPort(), responseClass); }

        public Response getTargetMain(String uri) { return getTargetMain(UriBuilder.fromPath(uri).build()); }

        public Response postTargetMain(URI uri, Object entity) { return postTarget(uri, getLocalPort(), entity); };

        public Response postTargetMain(String path, Object entity) { return postTargetMain(UriBuilder.fromPath(path).build(), entity); }

        public <T> T postTargetMain(String path, Object entity, Class<T> responseClass) { return postTarget(UriBuilder.fromPath(path).build(), getLocalPort(), entity, responseClass); };

        public Response postTargetAdmin(String uri, Object entity) { return postTarget(UriBuilder.fromPath(uri).build(), getAdminPort(), entity); }

        public Response postTarget(URI uri, int port, Object entity) {
            return client.target(buildUri(uri, port))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        }

        public <T> T postTarget(URI uri, int port, Object entity, Class<T> responseClass) {
            return client.target(buildUri(uri, port))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(
                            Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE),
                            responseClass
                    );
        }

        public Response getTarget(URI uri, int port) {
            return client.target(buildUri(uri, port))
                    .request()
                    .get();
        }

        public <T> T getTarget(URI uri, int port, Class<T> responseClass) {
            return client.target(buildUri(uri, port))
                    .request()
                    .get(responseClass);
        }

        private URI buildUri(URI baseUri, int port) {
            UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost").port(port).path(baseUri.getRawPath());
            if (baseUri.getQuery() != null) {
                uriBuilder.replaceQuery(baseUri.getQuery());
            }

            return uriBuilder.build();
        }
    }

}
