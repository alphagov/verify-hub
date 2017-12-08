package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromCountry;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.CountryMetadataRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.MetadataRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.integrationtest.hub.samlengine.support.AssertionDecrypter;
import uk.gov.ida.saml.core.extensions.EidasAuthnContext;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.idp.test.AuthnResponseFactory;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class CountryAuthnResponseTranslatorResourceTest {

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA256();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private final static String matchingServiceEntityId = TEST_RP_MS;
    public static final String DESTINATION = "http://localhost" + Urls.FrontendUrls.SAML2_SSO_EIDAS_RESPONSE_ENDPOINT;

    private static Client client;
    private AuthnResponseFactory authnResponseFactory;

    @ClassRule
    public static final ConfigStubRule configStubRule = new ConfigStubRule();

    @ClassRule
    public static final SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
        ConfigOverride.config("configUri", configStubRule.baseUri().build().toASCIIString()),
        ConfigOverride.config("country.saml.expectedDestination", DESTINATION)
    );

    @Before
    public void setUp() throws Exception {
        authnResponseFactory = AuthnResponseFactory.anAuthnResponseFactory();
        configStubRule.setupStubForCertificates(TEST_RP_MS);
    }

    @BeforeClass
    public static void setUpClient() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(CountryAuthnResponseTranslatorResourceTest.class.getSimpleName());
    }

    @Test
    public void shouldReturnSuccessResponse() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY);
        org.opensaml.saml.saml2.core.Response originalAuthnResponse = new HubTransformersFactory().getStringToResponseTransformer().apply(dto.getSamlResponse());

        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromCountry inboundResponseFromCountry = response.readEntity(InboundResponseFromCountry.class);
        assertThat(inboundResponseFromCountry.getStatus()).isEqualTo(Optional.of("Success"));
        assertThat(inboundResponseFromCountry.getIssuer()).isEqualTo(samlEngineAppRule.getCountryMetadataUri());
        assertThatDecryptedAssertionsAreTheSame(inboundResponseFromCountry, originalAuthnResponse);
    }

    @Test
    public void shouldReturnErrorWhenValidatingEidasAuthnResponseContainingInvalidSignature() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(TestCertificateStrings.METADATA_SIGNING_B_PUBLIC_CERT, TestCertificateStrings.METADATA_SIGNING_B_PRIVATE_KEY);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private void assertThatDecryptedAssertionsAreTheSame(InboundResponseFromCountry response, org.opensaml.saml.saml2.core.Response originalResponse) {
        AssertionDecrypter hubDecrypter = new AssertionDecrypter(TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY, TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT);
        List<Assertion> originalAssertions = hubDecrypter.decryptAssertions(originalResponse);

        AssertionDecrypter rpDecrypter = new AssertionDecrypter(TestCertificateStrings.TEST_RP_MS_PRIVATE_ENCRYPTION_KEY, TestCertificateStrings.TEST_RP_PUBLIC_ENCRYPTION_CERT);
        Assertion returnedAssertion = rpDecrypter.decryptAssertion(response.getEncryptedIdentityAssertionBlob().get());

        assertThat(originalAssertions).hasSize(1);
        Assertion originalAssertion = originalAssertions.get(0);
        assertEquals(returnedAssertion, originalAssertion);
    }

    private void assertEquals(Assertion first, Assertion second) {
        XmlObjectToBase64EncodedStringTransformer serializer = new XmlObjectToBase64EncodedStringTransformer();
        assertThat(serializer.apply(first)).isEqualTo(serializer.apply(second));
    }

    private SamlAuthnResponseTranslatorDto createAuthnResponseSignedByKeyPair(String publicKey, String privateKey) throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        String samlResponse = authnResponseFactory.aSamlResponseFromCountry("a-request",
            samlEngineAppRule.getCountryMetadataUri(),
            publicKey,
            privateKey,
            DESTINATION,
            SIGNATURE_ALGORITHM,
            DIGEST_ALGORITHM,
            EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
            EidasAuthnContext.EIDAS_LOA_SUBSTANTIAL,
            DESTINATION,
            samlEngineAppRule.getCountryMetadataUri());
        return new SamlAuthnResponseTranslatorDto(samlResponse, sessionId, "127.0.0.1", matchingServiceEntityId);
    }

    private Response postAuthnResponseToSamlEngine(SamlAuthnResponseTranslatorDto authnResponse) throws Exception {
        return postToSamlEngineAuthnResonseSignedBy(authnResponse, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE));
    }

    private Response postToSamlEngineAuthnResonseSignedBy(SamlAuthnResponseTranslatorDto dto, URI uri) {
        return client
                .target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(dto));
    }
}
