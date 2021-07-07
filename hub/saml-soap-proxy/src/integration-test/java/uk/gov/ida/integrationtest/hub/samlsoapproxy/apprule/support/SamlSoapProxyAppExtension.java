package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import certificates.values.CACertificates;
import httpstub.HttpStubExtension;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.Constants;
import uk.gov.ida.hub.samlsoapproxy.SamlSoapProxyApplication;
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

public class SamlSoapProxyAppExtension extends DropwizardAppExtension {
    private static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";
    private static final HttpStubExtension verifyMetadataServer = new HttpStubExtension();
    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();


    public SamlSoapProxyAppExtension(Class applicationClass, @Nullable String configPath, ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
    }

    public static void tearDown() {
        metadataTrustStore.delete();
        rpTrustStore.delete();
        hubTrustStore.delete();
        idpTrustStore.delete();
    }
    public static SamlSoapProxyAppExtensionBuilder builder() { return new SamlSoapProxyAppExtensionBuilder(); }

    public SamlSoapProxyClient getClient() {
        return new SamlSoapProxyClient();
    }

    public static class SamlSoapProxyAppExtensionBuilder {
        private ConfigOverride[] configOverrides = new ConfigOverride[]{};

        public SamlSoapProxyAppExtensionBuilder withConfigOverrides(ConfigOverride... overrides) {
            configOverrides = overrides;
            return this;
        }

        public SamlSoapProxyAppExtension build() {
            metadataTrustStore.create();
            hubTrustStore.create();
            idpTrustStore.create();
            rpTrustStore.create();
            CollectorRegistry.defaultRegistry.clear();

            try {
                InitializationService.initialize();

                verifyMetadataServer.start();
                verifyMetadataServer.reset();
                verifyMetadataServer.register(VERIFY_METADATA_PATH, 200, Constants.APPLICATION_SAMLMETADATA_XML, new MetadataFactory().defaultMetadata());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return new SamlSoapProxyAppExtension(
                    SamlSoapProxyApplication.class,
                    ResourceHelpers.resourceFilePath("saml-soap-proxy.yml"),
                    ArrayUtils.addAll(configOverrides, defaultConfigOverrides())
            );
        }
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
                config("metadata.hubTrustStore.path", hubTrustStore.getAbsolutePath()),
                config("metadata.hubTrustStore.password", hubTrustStore.getPassword()),
                config("metadata.idpTrustStore.path", idpTrustStore.getAbsolutePath()),
                config("metadata.idpTrustStore.password", idpTrustStore.getPassword()),
                config("metadata.uri", "http://localhost:" + verifyMetadataServer.getPort() + VERIFY_METADATA_PATH),
                config("metadata.expectedEntityId", HUB_ENTITY_ID),
                config("certificatesConfigCacheExpiry", "20s"),
                config("eventEmitterConfiguration.enabled", "false"),
        };
    }

    public class SamlSoapProxyClient {
        private Client client;

        public SamlSoapProxyClient() { client = client(); }

        public Response getTargetMain(URI uri) { return getTarget(uri, getLocalPort()); }

        public Response getTargetMain(String uri) { return getTargetMain(UriBuilder.fromPath(uri).build()); }

        public Response postTargetMain(URI uri, Object entity) { return postTarget(uri, getLocalPort(), entity); };

        public Response getTargetAdmin(String uri) { return getTarget(UriBuilder.fromPath(uri).build(), getAdminPort()); }

        public Response postTargetAdmin(String uri, Object entity) { return postTarget(UriBuilder.fromPath(uri).build(), getAdminPort(), entity); }

        public Response postTarget(URI uri, int port, Object entity) {
            return client.target(buildUri(uri, port))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        }

        public Response getTarget(URI uri, int port) {
            return client.target(buildUri(uri, port))
                    .request()
                    .get();
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
