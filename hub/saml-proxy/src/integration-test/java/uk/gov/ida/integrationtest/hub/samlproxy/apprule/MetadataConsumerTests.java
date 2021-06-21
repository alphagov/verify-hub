package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.hub.samlproxy.domain.ResponseActionDto;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.PolicyStubRule;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;
import uk.gov.ida.saml.core.test.AuthnResponseFactory;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY;

public class MetadataConsumerTests {

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA1();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private static final String ANALYTICS_SESSION_ID = UUID.randomUUID().toString();
    private static final String JOURNEY_TYPE = "some-journey-type";

    private static Client client;
    private AuthnResponseFactory authnResponseFactory;

    @ClassRule
    public static final PolicyStubRule policyStubRule = new PolicyStubRule();

    @ClassRule
    public static final SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule(ConfigOverride.config("policyUri", policyStubRule.baseUri().build().toASCIIString()));

    @Before
    public void setUp() {
        authnResponseFactory = AuthnResponseFactory.anAuthnResponseFactory();
    }

    @BeforeClass
    public static void setUpClient() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlProxyAppRule.getEnvironment()).using(jerseyClientConfiguration).build(SamlMessageReceiverApiResourceTest.class.getSimpleName());
    }

    @Test
    public void shouldAllowRequestsWhenMetadataIsAvailableAndValid() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        policyStubRule.register(UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId).getPath(), 200, ResponseActionDto.success(sessionId, true, LEVEL_2, null));
        org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory.aResponseFromIdp(
                TestEntityIds.STUB_IDP_ONE,
                STUB_IDP_PUBLIC_PRIMARY_CERT,
                STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                "",
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        final String samlResponseString = new XmlObjectToBase64EncodedStringTransformer<>().apply(samlResponse);

        ResponseActionDto post = postSAML(new SamlRequestDto(samlResponseString, sessionId.getSessionId(), "127.0.0.1", ANALYTICS_SESSION_ID, JOURNEY_TYPE))
                .readEntity(ResponseActionDto.class);

        assertThat(post.getSessionId()).isEqualTo(sessionId);
        assertThat(post.getLoaAchieved()).isEqualTo(LEVEL_2);
    }

    @Test
    public void shouldReturnBadRequestWhenEntityIdCannotBeFoundInMetadata() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();

        policyStubRule.register(UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId).getPath(), 200, ResponseActionDto.success(sessionId, true, LEVEL_2, null));
        org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory.aResponseFromIdp(
                "non-existent-entity-id",
                STUB_IDP_PUBLIC_PRIMARY_CERT,
                STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                "",
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        final String samlResponseString = new XmlObjectToBase64EncodedStringTransformer<>().apply(samlResponse);
        SamlRequestDto samlRequestDto = new SamlRequestDto(samlResponseString,
                sessionId.getSessionId(), "127.0.0.1", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        assertThat(postSAML(samlRequestDto).getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode
                ());
    }

    private Response postSAML(SamlRequestDto requestDto) {
        return client.target(samlProxyAppRule.getUri(Urls.SamlProxyUrls
                .SAML2_SSO_RECEIVER_API_RESOURCE)).request().post(Entity.entity(requestDto, MediaType
                .APPLICATION_JSON_TYPE));
    }
}
