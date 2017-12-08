package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import certificates.values.CACertificates;
import httpstub.HttpStubRule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.Constants;
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.saml.core.test.TestEntityIds;
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

public class SamlProxyAppRule extends DropwizardAppRule<SamlProxyConfiguration> {
    private static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";
    private static final String COUNTRY_METADATA_PATH = "/uk/gov/ida/saml/metadata/country";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();
    private static final HttpStubRule countryMetadataServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();

    public SamlProxyAppRule(ConfigOverride... configOverrides) {
        this(true, configOverrides);
    }

    public SamlProxyAppRule(boolean isCountryEnabled, ConfigOverride... configOverrides) {
        super(SamlProxyApplication.class,
                ResourceHelpers.resourceFilePath("saml-proxy.yml"),
                withDefaultOverrides(isCountryEnabled, configOverrides)
        );
    }

    public static ConfigOverride[] withDefaultOverrides(boolean isCountryEnabled, ConfigOverride ... configOverrides) {
        List<ConfigOverride> overrides = Stream.of(
                config("saml.entityId", HUB_ENTITY_ID),
                config("server.applicationConnectors[0].port", "0"),
                config("server.adminConnectors[0].port", "0"),
                config("clientTrustStoreConfiguration.path", clientTrustStore.getAbsolutePath()),
                config("clientTrustStoreConfiguration.password", clientTrustStore.getPassword()),
                config("rpTrustStoreConfiguration.path", rpTrustStore.getAbsolutePath()),
                config("rpTrustStoreConfiguration.password", rpTrustStore.getPassword()),
                config("metadata.trustStorePath", metadataTrustStore.getAbsolutePath()),
                config("metadata.trustStorePassword", metadataTrustStore.getPassword()),
                config("metadata.uri", "http://localhost:" + verifyMetadataServer.getPort() + VERIFY_METADATA_PATH)
        ).collect(Collectors.toList());

        if (isCountryEnabled) {
            List<ConfigOverride> countryOverrides = Stream.of(
                config("eidas", "true"),
                //config("country.metadata.uri", "http://localhost:" + countryMetadataServer.getPort() + COUNTRY_METADATA_PATH),
                config("country.metadata.trustStorePath", metadataTrustStore.getAbsolutePath()),
                config("country.metadata.trustStorePassword", metadataTrustStore.getPassword()),
                config("country.metadata.minRefreshDelay", "60000"),
                config("country.metadata.maxRefreshDelay", "600000"),
                //config("country.metadata.expectedEntityId", "http://thecountry/metadata"),
                config("country.metadata.jerseyClientName", "country-metadata-client"),
                config("country.metadata.client.timeout", "2s"),
                config("country.metadata.client.timeToLive", "10m"),
                config("country.metadata.client.cookiesEnabled", "false"),
                config("country.metadata.client.connectionTimeout", "1s"),
                config("country.metadata.client.retries", "3"),
                config("country.metadata.client.keepAlive", "60s"),
                config("country.metadata.client.chunkedEncodingEnabled", "false"),
                config("country.metadata.client.validateAfterInactivityPeriod", "5s"),
                config("country.metadata.client.tls.protocol", "TLSv1.2"),
                config("country.metadata.client.tls.verifyHostname", "false"),
                config("country.metadata.client.tls.trustSelfSignedCertificates", "true")
            ).collect(Collectors.toList());
            overrides.addAll(countryOverrides);
        }
        overrides.addAll(Arrays.asList(configOverrides));
        return overrides.toArray(new ConfigOverride[overrides.size()]);
    }

    @Override
    protected void before() {
        metadataTrustStore.create();
        clientTrustStore.create();
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
        clientTrustStore.delete();

        super.after();
    }

    public URI getUri(String path) {
        return UriBuilder.fromUri("http://localhost")
                .path(path)
                .port(getLocalPort())
                .build();
    }
}
