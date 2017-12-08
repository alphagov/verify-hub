package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import httpstub.HttpStubRule;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.PolicyStubRule;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;
import uk.gov.ida.integrationtest.hub.samlproxy.support.TestSamlRequestFactory;
import uk.gov.ida.saml.core.test.AuthnRequestFactory;
import uk.gov.ida.saml.core.test.AuthnRequestIdGenerator;
import uk.gov.ida.saml.hub.domain.Endpoints;
import uk.gov.ida.saml.idp.test.AuthnResponseFactory;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_PUBLIC_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder.anAuthnRequest;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.idp.test.AuthnResponseFactory.anAuthnResponseFactory;

public class SamlMessageReceiverApiResourceTest {
    private static final String RELAY_STATE = RandomStringUtils.randomAlphanumeric(10);
    private static final String INVALID_RELAY_STATE = RELAY_STATE + ">";
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA1();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private static Client client;

    @ClassRule
    public static PolicyStubRule policyStubRule = new PolicyStubRule();

    @ClassRule
    public static ConfigStubRule configStubRule = new ConfigStubRule();

    @ClassRule
    public static HttpStubRule eventSinkStubRule = new HttpStubRule();

    @Before
    public void resetStubRules() {
        configStubRule.reset();
        policyStubRule.reset();
        eventSinkStubRule.reset();
    }

    @ClassRule
    public static SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule(
            ConfigOverride.config("configUri", configStubRule.baseUri().build().toASCIIString()),
            ConfigOverride.config("policyUri", policyStubRule.baseUri().build().toASCIIString()),
            ConfigOverride.config("eventSinkUri", eventSinkStubRule.baseUri().build().toASCIIString()));

    private AuthnRequestFactory authnRequestFactory;
    private AuthnResponseFactory authnResponseFactory = anAuthnResponseFactory();
    private XmlObjectToBase64EncodedStringTransformer<AuthnRequest> authnRequestToStringTransformer;

    @Before
    public void setUp() throws Exception {
        authnRequestToStringTransformer = new XmlObjectToBase64EncodedStringTransformer<>();
        authnRequestFactory = new AuthnRequestFactory(authnRequestToStringTransformer);
    }

    @BeforeClass
    public static void setUpClient() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client =  new JerseyClientBuilder(samlProxyAppRule.getEnvironment()).using(jerseyClientConfiguration).build
                (SamlMessageReceiverApiResourceTest.class.getSimpleName());
        eventSinkStubRule.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
    }

    @Test
    public void responsePost_shouldRespondWithSuccessWhenPolicyRespondsWithSuccess() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        policyStubRule.receiveAuthnResponseFromIdp(sessionId, LevelOfAssurance.LEVEL_2);

        final String samlResponse = authnResponseFactory.aSamlResponseFromIdp(
                STUB_IDP_ONE, STUB_IDP_PUBLIC_PRIMARY_CERT,
                STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                Endpoints.SSO_RESPONSE_ENDPOINT,
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        SamlRequestDto authnResponse = new SamlRequestDto(samlResponse, sessionId, "127.0.0.1");

        final Response response = postSAML(authnResponse, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    public void shouldReturn400IfAuthnResponseIsSignedByAnRp() throws Exception {
        final String samlResponse = authnResponseFactory.aSamlResponseFromIdp(
                TEST_RP, TEST_RP_PUBLIC_SIGNING_CERT,
                TEST_RP_PRIVATE_SIGNING_KEY,
                Endpoints.SSO_RESPONSE_ENDPOINT,
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        SamlRequestDto authnResponse = new SamlRequestDto(samlResponse, "sessionId", "127.0.0.1");

        final Response response = postSAML(authnResponse, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void shouldReturn400IfAuthnRequestIsSignedByAnIdp() throws Exception {
        SamlRequestDto authnRequest = createAuthnRequest(STUB_IDP_ONE, "relayState", STUB_IDP_PUBLIC_PRIMARY_CERT, STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY);
        configStubRule.setupStubForNonExistentSigningCertificates(STUB_IDP_ONE);

        Response clientResponse = postSAML(authnRequest, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldCreateSessionForAuthnRequest() throws Exception {
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, "relayState", TEST_RP_PUBLIC_SIGNING_CERT, TEST_RP_PRIVATE_SIGNING_KEY);
        configStubRule.setupStubForCertificates(TEST_RP);
        SessionId sessionId = SessionId.createNewSessionId();
        policyStubRule.stubCreateSession(sessionId);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(clientResponse.readEntity(SessionId.class)).isEqualTo(sessionId);
    }

    @Test
    public void shouldReturnBadRequestAndShouldAuditWhenSendingAnAuthnRequestFromAnIncorectIssuer() throws Exception {
        SamlRequestDto authnRequest = createAuthnRequest(STUB_IDP_ONE, "relayState", TEST_PUBLIC_CERT, TEST_PRIVATE_KEY);
        configStubRule.setupStubForNonExistentSigningCertificates(STUB_IDP_ONE);
        eventSinkStubRule.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());

        assertThat(eventSinkStubRule.getCountOfRequestsTo(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE)).isEqualTo(0);

        Response clientResponse = postSAML(authnRequest, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(eventSinkStubRule.getCountOfRequestsTo(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE)).isEqualTo(1);
    }

    @Test
    public void shouldErrorWhenSamlStringIsTooSmall() throws Exception {
        SamlRequestDto authnRequestWrapper = new SamlRequestDto("too small", "relayState", "ipAddress");

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenASamlStringIsNull() throws Exception {
        SamlRequestDto authnRequestWrapper = new SamlRequestDto(null, "relayState", "ipAddress");

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenNonBase64SamlRequest() throws Exception {
        SamlRequestDto authnRequestWrapper = new SamlRequestDto(TestSamlRequestFactory.createNonBase64Request(), "relayState", "ipAddress");

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldReturnErrorWhenInvalidResponseFromIdp() throws Exception {
        org.opensaml.saml.saml2.core.Response idpAuthnResponse = aResponse()
                .withIssuer(anIssuer().withIssuerId(STUB_IDP_ONE).build())

                .withoutSignatureElement()
                .build();

        SamlRequestDto authnRequestWrapper = new SamlRequestDto(authnRequestToStringTransformer.apply(idpAuthnResponse), "relayState", "ipAddress");

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenAuthnRequestIsNotSigned() throws Exception {
        AuthnRequest authnRequest = anAuthnRequest()
                .withIssuer(anIssuer().withIssuerId(TEST_RP).build())
                .withDestination(Endpoints.SSO_REQUEST_ENDPOINT)
                .withId(AuthnRequestIdGenerator.generateRequestId())
                .withoutSignatureElement()
                .build();

        SamlRequestDto authnRequestWrapper = new SamlRequestDto(authnRequestToStringTransformer.apply(authnRequest), "relayState", "ipAddress");

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenRelayStateIsInvalid() throws Exception {
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, INVALID_RELAY_STATE, TEST_PUBLIC_CERT, TEST_PRIVATE_KEY);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenRelayStateIsMoreThanEightyCharacters() throws Exception {
        String longRelayState = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, longRelayState, TEST_PUBLIC_CERT, TEST_PRIVATE_KEY);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenAProblemOccursWithinSessionProxy() throws Exception {
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, RELAY_STATE, TEST_RP_PUBLIC_SIGNING_CERT,
                TEST_RP_PRIVATE_SIGNING_KEY);
        configStubRule.setupStubForCertificates(TEST_RP);

        policyStubRule.returnErrorForCreateSession();

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.NETWORK_ERROR);
    }

    @Test
    public void shouldErrorWhenAuthnRequestIsInvalid() throws Exception {
        String id = AuthnRequestIdGenerator.generateRequestId();
        Optional<Boolean> forceAuthentication = Optional.of(false);
        Optional<Integer> assertionConsumerServiceIndex = Optional.of(1);
        String issuer = TEST_RP;
        Optional<URI> assertionConsumerServiceUrl = Optional.empty();
        String anAuthnRequest = authnRequestFactory.anInvalidAuthnRequest(
                id,
                issuer,
                forceAuthentication,
                assertionConsumerServiceUrl,
                assertionConsumerServiceIndex,
                TEST_PUBLIC_CERT,
                TEST_PRIVATE_KEY,
                Endpoints.SSO_REQUEST_ENDPOINT,
                Optional.empty());
        SamlRequestDto authnRequestWrapper = new SamlRequestDto(anAuthnRequest, "relayState", "ipAddress");
        configStubRule.setupStubForCertificates(issuer);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(clientResponse, ExceptionType.INVALID_SAML);
    }

    private Response postSAML(SamlRequestDto requestDTO, String path) {
        return client.target(samlProxyAppRule.getUri(path)).request().post(Entity
                .entity(requestDTO, MediaType
                .APPLICATION_JSON_TYPE));
    }

    private void assertError(final Response response, final ExceptionType exceptionType) {
        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getErrorId()).isNotNull();
        assertThat(entity.getExceptionType()).isEqualTo(exceptionType);
    }

    private SamlRequestDto createAuthnRequest(String issuer, String relayState, String publicCert, String privateKey) throws JsonProcessingException {
        String id = AuthnRequestIdGenerator.generateRequestId();
        Optional<Boolean> forceAuthentication = Optional.of(false);
        Optional<Integer> assertionConsumerServiceIndex = Optional.of(1);
        Optional<URI> assertionConsumerServiceUrl = Optional.empty();
        String anAuthnRequest = authnRequestFactory.anAuthnRequest(
                id,
                issuer,
                forceAuthentication,
                assertionConsumerServiceUrl,
                assertionConsumerServiceIndex,
                publicCert,
                privateKey,
                Endpoints.SSO_REQUEST_ENDPOINT,
                Optional.empty());
        SamlRequestDto authnRequestWrapper = new SamlRequestDto(anAuthnRequest, relayState, "ipAddress");
        return authnRequestWrapper;
    }
}
