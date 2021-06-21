package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import certificates.values.CACertificates;
import httpstub.HttpStubRule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.prometheus.client.CollectorRegistry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.Constants;
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.dropwizard.testing.ConfigOverride.config;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlProxyAppRule extends DropwizardAppRule<SamlProxyConfiguration> {
    private static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";
    private static final String END_CERT = "\n-----END CERTIFICATE-----";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();

    public SamlProxyAppRule(ConfigOverride... configOverrides) {
        super(SamlProxyApplication.class,
                ResourceHelpers.resourceFilePath("saml-proxy.yml"),
                withDefaultOverrides(configOverrides)
        );
    }

    public static ConfigOverride[] withDefaultOverrides(ConfigOverride ... configOverrides) {
        List<ConfigOverride> overrides = Stream.of(
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
                config("eventEmitterConfiguration.enabled", "false")
        ).collect(Collectors.toList());

        overrides.addAll(Arrays.asList(configOverrides));
        return overrides.toArray(new ConfigOverride[0]);
    }

    @Override
    protected void before() {
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
