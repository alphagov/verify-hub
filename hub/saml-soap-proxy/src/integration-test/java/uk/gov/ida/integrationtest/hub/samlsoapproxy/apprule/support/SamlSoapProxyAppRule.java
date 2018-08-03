package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import certificates.values.CACertificates;
import httpstub.HttpStubRule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.Constants;
import uk.gov.ida.hub.samlsoapproxy.SamlSoapProxyApplication;
import uk.gov.ida.hub.samlsoapproxy.SamlSoapProxyConfiguration;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Throwables.propagate;
import static io.dropwizard.testing.ConfigOverride.config;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlSoapProxyAppRule extends DropwizardAppRule<SamlSoapProxyConfiguration> {
    private static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";
    private static final String COUNTRY_METADATA_PATH = "/uk/gov/ida/saml/metadata/country";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();
    private static final HttpStubRule countryMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();

    public SamlSoapProxyAppRule(ConfigOverride... configOverrides) {
        super(SamlSoapProxyApplication.class,
                io.dropwizard.testing.ResourceHelpers.resourceFilePath("saml-soap-proxy.yml"),
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
                config("metadata.hubTrustStore.path", hubTrustStore.getAbsolutePath()),
                config("metadata.hubTrustStore.password", hubTrustStore.getPassword()),
                config("metadata.idpTrustStore.path", idpTrustStore.getAbsolutePath()),
                config("metadata.idpTrustStore.password", idpTrustStore.getPassword()),
                config("metadata.uri", "http://localhost:" + verifyMetadataServer.getPort() + VERIFY_METADATA_PATH),
                config("metadata.expectedEntityId", HUB_ENTITY_ID)
        ).collect(Collectors.toList());

        overrides.addAll(Arrays.asList(configOverrides));
        return overrides.toArray(new ConfigOverride[overrides.size()]);
    }

    @Override
    protected void before() {
        metadataTrustStore.create();
        hubTrustStore.create();
        idpTrustStore.create();
        rpTrustStore.create();

        try {
            InitializationService.initialize();

            verifyMetadataServer.reset();
            verifyMetadataServer.register(VERIFY_METADATA_PATH, 200, Constants.APPLICATION_SAMLMETADATA_XML, new MetadataFactory().defaultMetadata());

            countryMetadataServer.reset();
            countryMetadataServer.register(COUNTRY_METADATA_PATH, 200, Constants.APPLICATION_SAMLMETADATA_XML, new MetadataFactory().defaultMetadata());
        } catch (Exception e) {
            throw propagate(e);
        }

        super.before();
    }

    @Override
    protected void after() {
        metadataTrustStore.delete();
        rpTrustStore.delete();
        hubTrustStore.delete();
        idpTrustStore.delete();

        super.after();
    }

    public URI getUri(String path) {
        return UriBuilder.fromUri("http://localhost")
                .path(path)
                .port(getLocalPort())
                .build();
    }
}
