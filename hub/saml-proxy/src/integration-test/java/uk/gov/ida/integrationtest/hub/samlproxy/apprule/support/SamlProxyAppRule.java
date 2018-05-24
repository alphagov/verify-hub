package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import certificates.values.CACertificates;
import com.nimbusds.jose.JOSEException;
import httpstub.HttpStubRule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.Constants;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.eidas.trustanchor.Generator;
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.IdpSsoDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Throwables.propagate;
import static io.dropwizard.testing.ConfigOverride.config;

import static uk.gov.ida.saml.core.test.TestCertificateStrings.METADATA_SIGNING_A_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.METADATA_SIGNING_A_PUBLIC_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.metadata.ResourceEncoder.entityIdAsResource;

public class SamlProxyAppRule extends DropwizardAppRule<SamlProxyConfiguration> {
    private static final String VERIFY_METADATA_PATH = "/uk/gov/ida/saml/metadata/federation";
    private static final String COUNTRY_METADATA_PATH = "/uk/gov/ida/saml/metadata/country";
    public static final String EIDAS_ENTITY_ID = "http://localhost/eidasMetadata";
    private static final String TRUST_ANCHOR_PATH = "/trust-anchor";
    private static final String METADATA_AGGREGATOR_PATH = "/metadata-aggregator";
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";
    private static final String END_CERT = "\n-----END CERTIFICATE-----";

    private static final HttpStubRule verifyMetadataServer = new HttpStubRule();
    private static final HttpStubRule metadataAggregatorServer = new HttpStubRule();
    private static final HttpStubRule trustAnchorServer = new HttpStubRule();

    private static final KeyStoreResource metadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();
    private static final KeyStoreResource hubTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("hubCA", CACertificates.TEST_CORE_CA).build();
    private static final KeyStoreResource idpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static final KeyStoreResource rpTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rpCA", CACertificates.TEST_RP_CA).build();
    private static final KeyStoreResource countryMetadataTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("idpCA", CACertificates.TEST_IDP_CA).withCertificate("metadataCA", CACertificates.TEST_METADATA_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();


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
                config("rpTrustStoreConfiguration.path", rpTrustStore.getAbsolutePath()),
                config("rpTrustStoreConfiguration.password", rpTrustStore.getPassword()),
                config("metadata.trustStore.path", metadataTrustStore.getAbsolutePath()),
                config("metadata.trustStore.password", metadataTrustStore.getPassword()),
                config("metadata.uri", "http://localhost:" + verifyMetadataServer.getPort() + VERIFY_METADATA_PATH),
                config("metadata.hubTrustStore.path", hubTrustStore.getAbsolutePath()),
                config("metadata.hubTrustStore.password", hubTrustStore.getPassword()),
                config("metadata.idpTrustStore.path", idpTrustStore.getAbsolutePath()),
                config("metadata.idpTrustStore.password", idpTrustStore.getPassword())
        ).collect(Collectors.toList());

        if (isCountryEnabled) {
            List<ConfigOverride> countryOverrides = Stream.of(
                    config("country.metadata.trustAnchorUri", "http://localhost:" + trustAnchorServer.getPort() + TRUST_ANCHOR_PATH),
                    config("country.metadata.metadataSourceUri", "http://localhost:" + metadataAggregatorServer.getPort() + METADATA_AGGREGATOR_PATH),
                    config("country.metadata.trustStore.store", countryMetadataTrustStore.getAbsolutePath()),
                    config("country.metadata.trustStore.trustStorePassword", countryMetadataTrustStore.getPassword()),
                    config("country.metadata.minRefreshDelay", "6000"),
                    config("country.metadata.maxRefreshDelay", "600000"),
                    config("country.metadata.trustAnchorMinRefreshDelay", "6000"),
                    config("country.metadata.trustAnchorMaxRefreshDelay", "600000"),
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
        hubTrustStore.create();
        idpTrustStore.create();
        rpTrustStore.create();
        countryMetadataTrustStore.create();

        try {
            InitializationService.initialize();
            String testCountryMetadata = new MetadataFactory().singleEntityMetadata(buildTestCountryEntityDescriptor());

            verifyMetadataServer.reset();
            verifyMetadataServer.register(VERIFY_METADATA_PATH, 200, Constants.APPLICATION_SAMLMETADATA_XML, new MetadataFactory().defaultMetadata());

            metadataAggregatorServer.reset();
            metadataAggregatorServer.register(
                    String.format("%s/%s", METADATA_AGGREGATOR_PATH, entityIdAsResource(COUNTRY_METADATA_PATH)),
                    200,
                    Constants.APPLICATION_SAMLMETADATA_XML,
                    testCountryMetadata);

            trustAnchorServer.reset();
            trustAnchorServer.register(TRUST_ANCHOR_PATH, 200, MediaType.APPLICATION_OCTET_STREAM, buildTrustAnchorString());
        } catch (Exception e) {
            throw propagate(e);
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

    public String getCountyEntityId(){
        return COUNTRY_METADATA_PATH;
    }

    public URI getUri(String path) {
        return UriBuilder.fromUri("http://localhost")
                .path(path)
                .port(getLocalPort())
                .build();
    }

    private String buildTrustAnchorString() throws ParseException, JOSEException, CertificateEncodingException {
        X509CertificateFactory x509CertificateFactory = new X509CertificateFactory();
        PrivateKey trustAnchorKey = new PrivateKeyFactory().createPrivateKey(Base64.decodeBase64(TestCertificateStrings.METADATA_SIGNING_A_PRIVATE_KEY));
        X509Certificate trustAnchorCert = new X509CertificateFactory().createCertificate(TestCertificateStrings.METADATA_SIGNING_A_PUBLIC_CERT);
        Generator generator = new Generator(trustAnchorKey, trustAnchorCert);
        HashMap<String, List<X509Certificate>> trustAnchorMap = new HashMap<>();
        X509Certificate metadataCACert = x509CertificateFactory.createCertificate(CACertificates.TEST_METADATA_CA.replace(BEGIN_CERT, "").replace(END_CERT, "").replace("\n", ""));
        trustAnchorMap.put(COUNTRY_METADATA_PATH, Collections.singletonList(metadataCACert));
        return generator.generateFromMap(trustAnchorMap).serialize();
    }

    private EntityDescriptor buildTestCountryEntityDescriptor() throws Exception {
        KeyDescriptor signingKeyDescriptor = KeyDescriptorBuilder.aKeyDescriptor()
                .withX509ForSigning(STUB_IDP_PUBLIC_PRIMARY_CERT)
                .build();

        IDPSSODescriptor idpSsoDescriptor = IdpSsoDescriptorBuilder.anIdpSsoDescriptor()
                .withoutDefaultSigningKey()
                .addKeyDescriptor(signingKeyDescriptor)
                .build();

        Signature signature = SignatureBuilder.aSignature()
                .withSigningCredential(new TestCredentialFactory(METADATA_SIGNING_A_PUBLIC_CERT, METADATA_SIGNING_A_PRIVATE_KEY).getSigningCredential())
                .withX509Data(METADATA_SIGNING_A_PUBLIC_CERT)
                .build();

        return EntityDescriptorBuilder.anEntityDescriptor()
                .withEntityId(COUNTRY_METADATA_PATH)
                .withIdpSsoDescriptor(idpSsoDescriptor)
                .setAddDefaultSpServiceDescriptor(false)
                .withValidUntil(DateTime.now().plusWeeks(2))
                .withSignature(signature)
                .build();
    }
}
