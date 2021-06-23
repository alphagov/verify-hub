package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.hub.samlproxy.domain.ResponseActionDto;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.PolicyStubExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension.SamlProxyClient;
import uk.gov.ida.saml.core.test.AuthnResponseFactory;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.util.UUID;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY;

public class MetadataConsumerTests {

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA1();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private static final String ANALYTICS_SESSION_ID = UUID.randomUUID().toString();
    private static final String JOURNEY_TYPE = "some-journey-type";

    private static AuthnResponseFactory authnResponseFactory;

    @Order(0)
    @RegisterExtension
    public static final PolicyStubExtension POLICY_STUB_EXTENSION = new PolicyStubExtension();

    @Order(1)
    @RegisterExtension
    public static final SamlProxyAppExtension samlProxyApp = SamlProxyAppExtension.builder()
            .withConfigOverrides(
                    config("policyUri", () -> POLICY_STUB_EXTENSION.baseUri().build().toASCIIString())
            )
            .build();

    private SamlProxyClient client;

    @BeforeAll
    public static void beforeClass() {
        authnResponseFactory = AuthnResponseFactory.anAuthnResponseFactory();
    }

    @BeforeEach
    public void beforeEach() {
        client = samlProxyApp.getClient();
    }

    @AfterAll
    public static void tearDown() {
        samlProxyApp.tearDown();
    }

    @Test
    public void shouldAllowRequestsWhenMetadataIsAvailableAndValid() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        POLICY_STUB_EXTENSION.register(UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId).getPath(), 200, ResponseActionDto.success(sessionId, true, LEVEL_2, null));
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

        POLICY_STUB_EXTENSION.register(UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId).getPath(), 200, ResponseActionDto.success(sessionId, true, LEVEL_2, null));
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
        return client.postTargetMain(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE, requestDto);
    }
}
