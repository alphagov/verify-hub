package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.hub.samlproxy.domain.ResponseActionDto;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.PolicyStubRule;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;
import uk.gov.ida.saml.core.test.AuthnResponseFactory;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.UUID;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_SECONDARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_SECONDARY_PRIVATE_KEY;

public class CountryMetadataConsumerTest {

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA256();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();

    private static final String idpSigningCert = STUB_IDP_PUBLIC_PRIMARY_CERT;
    private static final String idpSigningKey = STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY;
    private static final String anotherIdpSigningCert = STUB_IDP_PUBLIC_SECONDARY_CERT;
    private static final String anotherIdpSigningKey = STUB_IDP_PUBLIC_SECONDARY_PRIVATE_KEY;


    private static Client client;
    private String analyticsSessionId = UUID.randomUUID().toString();
    private String journeyType = "some-journey-type";
    private AuthnResponseFactory authnResponseFactory;

    @ClassRule
    public static final ConfigStubRule configStubRule = new ConfigStubRule();

    @ClassRule
    public static PolicyStubRule policyStubRule = new PolicyStubRule();

    @ClassRule
    public static final SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule(
        config("policyUri", policyStubRule.baseUri().build().toASCIIString()),
        config("configUri", configStubRule.baseUri().build().toASCIIString())
    );

    @Before
    public void setUp() {
        authnResponseFactory = AuthnResponseFactory.anAuthnResponseFactory();
    }

    @BeforeClass
    public static void setUpClient() {
        client = JerseyClientBuilder.createClient();
    }

    @Test
    public void shouldServeCountryMetadata() throws Exception {
        // Given
        SessionId sessionId = SessionId.createNewSessionId();
        policyStubRule.receiveAuthnResponseFromCountry(sessionId.toString(), LEVEL_2);
        org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory.aResponseFromIdp("a-request",
                samlProxyAppRule.getCountyEntityId(),
                idpSigningCert,
                idpSigningKey,
                "",
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        final String samlResponseString = new XmlObjectToBase64EncodedStringTransformer<>().apply(samlResponse);

        // When
        ResponseActionDto post = postSAML(new SamlRequestDto(samlResponseString, sessionId.getSessionId(), "127.0.0.1", analyticsSessionId, journeyType))
                .readEntity(ResponseActionDto.class);

        // Then
        assertThat(post.getSessionId()).isEqualTo(sessionId);
        assertThat(post.getLoaAchieved()).isEqualTo(LEVEL_2);
    }

    @Test
    public void shouldReturnErrorWhenValidatingEidasAuthnResponseContainingInvalidSignature() throws Exception {
        // Given
        SessionId sessionId = SessionId.createNewSessionId();
        org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory.aResponseFromIdp("a-request",
                samlProxyAppRule.getCountyEntityId(),
                anotherIdpSigningCert,
                anotherIdpSigningKey,
                "",
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        final String samlResponseString = new XmlObjectToBase64EncodedStringTransformer<>().apply(samlResponse);

        // When
        Response responseFromSamlProxy = postSAML(new SamlRequestDto(samlResponseString, sessionId.getSessionId(), "127.0.0.1", analyticsSessionId, journeyType));

        // Then
        assertThat(responseFromSamlProxy.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private javax.ws.rs.core.Response postSAML(SamlRequestDto requestDto) {
        return client.target(samlProxyAppRule.getUri(Urls.SamlProxyUrls.EIDAS_SAML2_SSO_RECEIVER_API_RESOURCE))
                .request()
                .post(Entity.entity(requestDto, MediaType.APPLICATION_JSON_TYPE));
    }
}
