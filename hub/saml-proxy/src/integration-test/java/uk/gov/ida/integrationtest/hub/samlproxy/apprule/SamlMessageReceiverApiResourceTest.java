package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import httpstub.HttpStubExtension;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.PolicyStubExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.support.TestSamlRequestFactory;
import uk.gov.ida.saml.core.test.AuthnRequestFactory;
import uk.gov.ida.saml.core.test.AuthnRequestIdGenerator;
import uk.gov.ida.saml.core.test.AuthnResponseFactory;
import uk.gov.ida.saml.hub.domain.Endpoints;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.AuthnResponseFactory.anAuthnResponseFactory;
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

public class SamlMessageReceiverApiResourceTest {
    private static final String RELAY_STATE = RandomStringUtils.randomAlphanumeric(10);
    private static final String INVALID_RELAY_STATE = RELAY_STATE + ">";
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA1();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private static final String ANALYTICS_SESSION_ID = UUID.randomUUID().toString();
    private static final String JOURNEY_TYPE = "some-journey-type";

    private static ClientSupport client;

    @Order(0)
    @RegisterExtension
    public static final PolicyStubExtension policyStub = new PolicyStubExtension();

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(0)
    @RegisterExtension
    public static HttpStubExtension eventSinkStub = new HttpStubExtension();

    @Order(1)
    @RegisterExtension
    public static TestDropwizardAppExtension samlProxyApp = SamlProxyAppExtension.forApp(SamlProxyApplication.class)
            .withDefaultConfigOverridesAnd()
            .configOverride("configUri", () -> configStub.baseUri().build().toASCIIString())
            .configOverride("policyUri", () -> policyStub.baseUri().build().toASCIIString())
            .configOverride("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("saml-proxy.yml"))
            .randomPorts()
            .create();

    private final AuthnResponseFactory authnResponseFactory = anAuthnResponseFactory();

    private AuthnRequestFactory authnRequestFactory;
    private XmlObjectToBase64EncodedStringTransformer<AuthnRequest> authnRequestToStringTransformer;

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
        eventSinkStub.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());

    }
    
    @BeforeEach
    public void setUp() {
        configStub.reset();
        policyStub.reset();
        eventSinkStub.reset();
        authnRequestToStringTransformer = new XmlObjectToBase64EncodedStringTransformer<>();
        authnRequestFactory = new AuthnRequestFactory(authnRequestToStringTransformer);
    }

    @AfterAll
    public static void tearDown() {
        SamlProxyAppExtension.tearDown();
    }
    
    @Test
    public void responsePost_shouldRespondWithSuccessWhenPolicyRespondsWithSuccess() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        policyStub.receiveAuthnResponseFromIdp(sessionId, LevelOfAssurance.LEVEL_2);

        org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory.aResponseFromIdp(
                STUB_IDP_ONE, STUB_IDP_PUBLIC_PRIMARY_CERT,
                STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                Endpoints.SSO_RESPONSE_ENDPOINT,
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        final String samlResponseString = new XmlObjectToBase64EncodedStringTransformer<>().apply(samlResponse);
        SamlRequestDto authnResponse = new SamlRequestDto(samlResponseString, sessionId, "127.0.0.1", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        final Response response = postSAML(authnResponse, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    public void shouldReturn400IfAuthnResponseIsSignedByAnRp() throws Exception {
        org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory.aResponseFromIdp(
                TEST_RP, TEST_RP_PUBLIC_SIGNING_CERT,
                TEST_RP_PRIVATE_SIGNING_KEY,
                Endpoints.SSO_RESPONSE_ENDPOINT,
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM);
        final String samlResponseString = new XmlObjectToBase64EncodedStringTransformer<>().apply(samlResponse);
        SamlRequestDto authnResponse = new SamlRequestDto(samlResponseString, "sessionId", "127.0.0.1", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        final Response response = postSAML(authnResponse, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void shouldReturn400IfAuthnRequestIsSignedByAnIdp() {
        SamlRequestDto authnRequest = createAuthnRequest(STUB_IDP_ONE, "relayState", STUB_IDP_PUBLIC_PRIMARY_CERT, STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY);
        configStub.setupStubForNonExistentSigningCertificates(STUB_IDP_ONE);

        Response clientResponse = postSAML(authnRequest, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldCreateSessionForAuthnRequest() throws Exception {
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, "relayState", TEST_RP_PUBLIC_SIGNING_CERT, TEST_RP_PRIVATE_SIGNING_KEY);
        configStub.setupStubForCertificates(TEST_RP);
        SessionId sessionId = SessionId.createNewSessionId();
        policyStub.stubCreateSession(sessionId);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(clientResponse.readEntity(SessionId.class)).isEqualTo(sessionId);
    }

    @Test
    public void shouldReturnBadRequestAndShouldAuditWhenSendingAnAuthnRequestFromAnIncorectIssuer() {
        SamlRequestDto authnRequest = createAuthnRequest(STUB_IDP_ONE, "relayState", TEST_PUBLIC_CERT, TEST_PRIVATE_KEY);
        configStub.setupStubForNonExistentSigningCertificates(STUB_IDP_ONE);
        eventSinkStub.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());

        assertThat(eventSinkStub.getCountOfRequestsTo(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE)).isEqualTo(0);

        Response clientResponse = postSAML(authnRequest, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(eventSinkStub.getCountOfRequestsTo(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE)).isEqualTo(1);
    }

    @Test
    public void shouldErrorWhenSamlStringIsTooSmall() {
        SamlRequestDto authnRequestWrapper = new SamlRequestDto("too small", "relayState", "ipAddress", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.BAD_REQUEST ,clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenASamlStringIsNull() {
        SamlRequestDto authnRequestWrapper = new SamlRequestDto(null, "relayState", "ipAddress", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.BAD_REQUEST, clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenNonBase64SamlRequest() {
        SamlRequestDto authnRequestWrapper = new SamlRequestDto(TestSamlRequestFactory.createNonBase64Request(), "relayState", "ipAddress", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.BAD_REQUEST, clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldReturnErrorWhenInvalidResponseFromIdp() throws Exception {
        org.opensaml.saml.saml2.core.Response idpAuthnResponse = aResponse()
                .withIssuer(anIssuer().withIssuerId(STUB_IDP_ONE).build())

                .withoutSignatureElement()
                .build();

        SamlRequestDto authnRequestWrapper = new SamlRequestDto(authnRequestToStringTransformer.apply(idpAuthnResponse), "relayState", "ipAddress", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);

        assertError(Response.Status.BAD_REQUEST, clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenAuthnRequestIsNotSigned() {
        AuthnRequest authnRequest = anAuthnRequest()
                .withIssuer(anIssuer().withIssuerId(TEST_RP).build())
                .withDestination(Endpoints.SSO_REQUEST_ENDPOINT)
                .withId(AuthnRequestIdGenerator.generateRequestId())
                .withoutSignatureElement()
                .build();

        SamlRequestDto authnRequestWrapper = new SamlRequestDto(authnRequestToStringTransformer.apply(authnRequest), "relayState", "ipAddress", ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.BAD_REQUEST, clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenRelayStateIsInvalid() {
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, INVALID_RELAY_STATE, TEST_PUBLIC_CERT, TEST_PRIVATE_KEY);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.BAD_REQUEST, clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenRelayStateIsMoreThanEightyCharacters() {
        String longRelayState = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, longRelayState, TEST_PUBLIC_CERT, TEST_PRIVATE_KEY);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.BAD_REQUEST, clientResponse, ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldErrorWhenAProblemOccursWithinSessionProxy() throws Exception {
        SamlRequestDto authnRequestWrapper = createAuthnRequest(TEST_RP, RELAY_STATE, TEST_RP_PUBLIC_SIGNING_CERT,
                TEST_RP_PRIVATE_SIGNING_KEY);
        configStub.setupStubForCertificates(TEST_RP);

        policyStub.returnErrorForCreateSession();

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.INTERNAL_SERVER_ERROR, clientResponse, ExceptionType.NETWORK_ERROR);
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
        SamlRequestDto authnRequestWrapper = new SamlRequestDto(anAuthnRequest, "relayState", "ipAddress", ANALYTICS_SESSION_ID, JOURNEY_TYPE);
        configStub.setupStubForCertificates(issuer);

        Response clientResponse = postSAML(authnRequestWrapper, Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);

        assertError(Response.Status.BAD_REQUEST, clientResponse, ExceptionType.INVALID_SAML);
    }

    private Response postSAML(SamlRequestDto requestDTO, String path) {
        return client.targetMain(path).request().post(Entity
                .entity(requestDTO, MediaType
                .APPLICATION_JSON_TYPE));
    }

    private void assertError(Response.StatusType statusType, final Response response, final ExceptionType exceptionType) {
        assertThat(response.getStatus()).isEqualTo(statusType.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getErrorId()).isNotNull();
        assertThat(entity.getExceptionType()).isEqualTo(exceptionType);
    }

    private SamlRequestDto createAuthnRequest(String issuer, String relayState, String publicCert, String privateKey) {
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

        return new SamlRequestDto(anAuthnRequest, relayState, "ipAddress", ANALYTICS_SESSION_ID, JOURNEY_TYPE);
    }
}
