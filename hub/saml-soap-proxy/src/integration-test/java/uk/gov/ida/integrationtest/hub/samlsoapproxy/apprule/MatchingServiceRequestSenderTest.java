package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import httpstub.ExpectedRequest;
import httpstub.RecordedRequest;
import httpstub.RequestAndResponse;
import httpstub.builders.ExpectedRequestBuilder;
import io.dropwizard.util.Duration;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.configuration.JerseyClientWithRetryBackoffConfiguration;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.builders.AttributeQueryContainerDtoBuilder;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlsoapproxy.exceptions.InvalidSamlRequestInAttributeQueryException;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.PolicyStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppRule;
import uk.gov.ida.restclient.ClientProvider;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder;
import uk.gov.ida.saml.core.test.builders.IssuerBuilder;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.getAllScenarios;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static io.dropwizard.testing.ConfigOverride.config;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlsoapproxy.Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubRule.msaStubRule;
import static uk.gov.ida.jerseyclient.JerseyClientWithRetryBackoffHandlerConfigurationBuilder.aJerseyClientWithRetryBackoffHandlerConfiguration;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
public class MatchingServiceRequestSenderTest {
    
    public static final Credential msaSigningCredential =  new TestCredentialFactory(TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PRIVATE_SIGNING_KEY).getSigningCredential();
    
    public static final Credential hubSigningCredential = new TestCredentialFactory(HUB_TEST_PUBLIC_SIGNING_CERT, HUB_TEST_PRIVATE_SIGNING_KEY).getSigningCredential();

    private static final String attibute_query_resource = "/attribute-query-request";
    
    private static Client client;

    private static String soapResponse;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStubRule = new EventSinkStubRule();

    @ClassRule
    public static MsaStubRule msaStubRule = msaStubRule();

    @ClassRule
    public static PolicyStubRule policyStubRule = new PolicyStubRule();

    @ClassRule
    public static SamlSoapProxyAppRule samlSoapProxyAppRule = new SamlSoapProxyAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString()),
            config("eventSinkUri", eventSinkStubRule.baseUri().build().toASCIIString()),
            config("policyUri", policyStubRule.baseUri().build().toASCIIString())
    );

    @Rule
    public WireMockRule errorSimulationRule = new WireMockRule(0);

    @Before
    public void setUp() throws Exception {
        policyStubRule.reset();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        final List<String> exceptionsToCatch = List.of(ConnectTimeoutException.class.getName(), SocketException.class.getName(), SocketTimeoutException.class.getName(), NoHttpResponseException.class.getName());
        JerseyClientWithRetryBackoffConfiguration jerseyClientConfiguration = aJerseyClientWithRetryBackoffHandlerConfiguration()
            .withTimeout(Duration.seconds(1))
            .withRetryBackoffPeriod(Duration.seconds(1))
            .withRetryExceptionList(exceptionsToCatch)
            .withChunkedEncodingEnabled(false)
            .withNumRetries(2)
            .build();

        ClientProvider provider = new ClientProvider(samlSoapProxyAppRule.getEnvironment(), jerseyClientConfiguration, true, MatchingServiceRequestSenderTest.class.getSimpleName());

        client = provider.get();
        eventSinkStubRule.setupStubForLogging();
        configStub.setupStubForCertificates(TEST_RP_MS);
        soapResponse = createMsaResponse();
        RequestAndResponse requestAndResponse = ExpectedRequestBuilder.expectRequest().withPath(attibute_query_resource).andWillRespondWith().withStatus(200).withBody(soapResponse).withContentType(MediaType.TEXT_XML_TYPE.toString()).build();
        msaStubRule.register(requestAndResponse);
    }

    private static String createMsaResponse() throws MarshallingException, SignatureException {
        Credential signingCredential = msaSigningCredential;
        org.opensaml.saml.saml2.core.Response response = aResponse().withIssuer(IssuerBuilder.anIssuer().withIssuerId(TEST_RP_MS).build()).withSigningCredential(signingCredential).build();
        Document soapEnvelope = new SoapMessageManager().wrapWithSoapEnvelope(new XmlObjectToElementTransformer<>().apply(response));
        return XmlUtils.writeToString(soapEnvelope);
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void sendHubMatchingServiceRequest_shouldAcceptAValidRequest() {
        Credential signingCredential = hubSigningCredential;
        AttributeQueryContainerDto attributeQueryContainerDto = AttributeQueryContainerDtoBuilder
                .anAttributeQueryContainerDto(AttributeQueryBuilder.anAttributeQuery().withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build()).withIssuer(IssuerBuilder.anIssuer().withIssuerId(HUB_ENTITY_ID).build()).build())
                .withIssuerId(HUB_ENTITY_ID)
                .withMatchingServiceUri(msaStubRule.getAttributeQueryRequestUri())
                .build();

        SessionId sessionId = SessionId.createNewSessionId();
        final URI uri = UriBuilder.fromUri(samlSoapProxyAppRule.getUri(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_REQUEST_SENDER_RESOURCE))
                .queryParam(Urls.SharedUrls.SESSION_ID_PARAM, sessionId)
                .build();

        String path = UriBuilder.fromPath(ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId).getPath();
        policyStubRule.register(path, 200);
        Response response = makepost(attributeQueryContainerDto, uri);

        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        andPolicyShouldReceiveASuccess(sessionId);
    }

    // When a bad request is made, it is nevertheless accepted - these bad requests are unit tested in
    // AttributeQueryRequestRunnableTest
    // So, this test is probably of dubious value but probably worth keeping given we have already spun up the
    // SamlSoapProxyAppRule
    @Test
    public void sendHubMatchingServiceRequest_shouldErrorIfRequestIsBad_wrongIssuer() {
        AttributeQueryContainerDto attributeQueryContainerDto = AttributeQueryContainerDtoBuilder
                .anAttributeQueryContainerDto(AttributeQueryBuilder.anAttributeQuery().withIssuer(IssuerBuilder.anIssuer().withIssuerId(HUB_ENTITY_ID).build()).build())
                .withIssuerId(TEST_RP)
                .withMatchingServiceUri(msaStubRule.getAttributeQueryRequestUri())
                .build();

        SessionId sessionId = SessionId.createNewSessionId();
        final URI uri = UriBuilder.fromUri(samlSoapProxyAppRule.getUri(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_REQUEST_SENDER_RESOURCE))
                .queryParam(Urls.SharedUrls.SESSION_ID_PARAM, sessionId)
                .build();
        Response response = makepost(attributeQueryContainerDto, uri);

        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        andPolicyShouldReceiveAFailure(sessionId);
    }

    @Test
    public void socketTimeoutRetryWithBackoffTests_thirdCallSucceeds() {
        final String firstCallState = "Call 1 Complete";
        final String secondCallState = "Call 2 Complete";
        final String thirdCallState = "Call 3 Complete";
        final String scenarioName = "socket timeout scenario";

        errorSimulationRule.stubFor(post(urlEqualTo(attibute_query_resource)).inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(2000)
                        .withStatus(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.TEXT_XML_TYPE.toString())
                        .withBody(soapResponse))
                .willSetStateTo(firstCallState)
        );

        errorSimulationRule.stubFor(post(urlEqualTo(attibute_query_resource)).inScenario(scenarioName)
                .whenScenarioStateIs(firstCallState)
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(2000)
                        .withStatus(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.TEXT_XML_TYPE.toString())
                        .withBody(soapResponse))
                .willSetStateTo(secondCallState)
        );

        errorSimulationRule.stubFor(post(urlEqualTo(attibute_query_resource)).inScenario(scenarioName)
                .whenScenarioStateIs(secondCallState)
                .willReturn(WireMock.aResponse()
                        .withStatus(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.TEXT_XML_TYPE.toString())
                        .withBody(soapResponse))
                .willSetStateTo(thirdCallState)
        );

        Credential signingCredential = hubSigningCredential;
        AttributeQueryContainerDto attributeQueryContainerDto = AttributeQueryContainerDtoBuilder
                .anAttributeQueryContainerDto(AttributeQueryBuilder.anAttributeQuery().withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build()).withIssuer(IssuerBuilder.anIssuer().withIssuerId(HUB_ENTITY_ID).build()).build())
                .withIssuerId(HUB_ENTITY_ID)
                .withMatchingServiceUri(msaStubRule.getAttributeQueryRequestUri())
                .build();

        long start = System.currentTimeMillis();

        SoapMessageManager soapMessageManager = new SoapMessageManager();
        Document requestDocument = soapMessageManager.wrapWithSoapEnvelope(convertToElementAndValidate(attributeQueryContainerDto));
        Entity entity = Entity.xml(requestDocument);

        Response response = client.target(URI.create(format("http://localhost:%d%s", errorSimulationRule.port(), attibute_query_resource)))
                .request(MediaType.TEXT_XML_TYPE).post(entity);
        long end = System.currentTimeMillis();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getScenario(scenarioName).getState()).isEqualTo(thirdCallState);
        assertThat((end - start)).isGreaterThanOrEqualTo(getTotalBackoffPeriod(2, Duration.milliseconds(1000)));
    }

    @Test
    public void socketTimeoutRetryWithBackoffTests_thirdCallFailsAndThrowsException() {
        final String firstCallState = "Call 1 Complete";
        final String secondCallState = "Call 2 Complete";
        final String thirdCallState = "Call 3 Complete";
        final String scenarioName = "socket timeout scenario";

        errorSimulationRule.stubFor(post(urlEqualTo(attibute_query_resource)).inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(2000)
                        .withStatus(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.TEXT_XML_TYPE.toString())
                        .withBody(soapResponse))
                .willSetStateTo(firstCallState)
        );

        errorSimulationRule.stubFor(post(urlEqualTo(attibute_query_resource)).inScenario(scenarioName)
                .whenScenarioStateIs(firstCallState)
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(2000)
                        .withStatus(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.TEXT_XML_TYPE.toString())
                        .withBody(soapResponse))
                .willSetStateTo(secondCallState)
        );

        errorSimulationRule.stubFor(post(urlEqualTo(attibute_query_resource)).inScenario(scenarioName)
                .whenScenarioStateIs(secondCallState)
                .willReturn(WireMock.aResponse()
                        .withStatus(Response.Status.OK.getStatusCode())
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", MediaType.TEXT_XML_TYPE.toString())
                        .withBody(soapResponse))
                .willSetStateTo(thirdCallState)
        );

        Credential signingCredential = hubSigningCredential;
        AttributeQueryContainerDto attributeQueryContainerDto = AttributeQueryContainerDtoBuilder
                .anAttributeQueryContainerDto(AttributeQueryBuilder.anAttributeQuery().withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build()).withIssuer(IssuerBuilder.anIssuer().withIssuerId(HUB_ENTITY_ID).build()).build())
                .withIssuerId(HUB_ENTITY_ID)
                .withMatchingServiceUri(msaStubRule.getAttributeQueryRequestUri())
                .build();

        long start = System.currentTimeMillis();

        SoapMessageManager soapMessageManager = new SoapMessageManager();
        Document requestDocument = soapMessageManager.wrapWithSoapEnvelope(convertToElementAndValidate(attributeQueryContainerDto));
        Entity entity = Entity.xml(requestDocument);
        try {
            client.target(URI.create(format("http://localhost:%d%s", errorSimulationRule.port(), attibute_query_resource)))
                    .request(MediaType.TEXT_XML_TYPE).post(entity);
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(ProcessingException.class);
            assertThat(ex.getCause()).isInstanceOf(SocketTimeoutException.class);
        }
        long end = System.currentTimeMillis();
        assertThat(getScenario(scenarioName).getState()).isEqualTo(thirdCallState);
        assertThat((end - start)).isGreaterThanOrEqualTo(getTotalBackoffPeriod(2, Duration.milliseconds(1000)));
    }

    private void andPolicyShouldReceiveASuccess(SessionId sessionId) {
        andPolicyShouldReceiveTheResult(sessionId, ATTRIBUTE_QUERY_RESPONSE_RESOURCE);
    }

    private void andPolicyShouldReceiveAFailure(SessionId sessionId) {
        andPolicyShouldReceiveTheResult(sessionId, Urls.PolicyUrls.MATCHING_SERVICE_REQUEST_FAILURE_RESOURCE);
    }

    private void andPolicyShouldReceiveTheResult(SessionId sessionId, String resultPath) {
        await().atMost(5, TimeUnit.SECONDS).until(() -> !policyStubRule.getRecordedRequest().isEmpty());
        RecordedRequest recordedRequest = policyStubRule.getLastRequest();
        String path = UriBuilder.fromPath(resultPath).build(sessionId).getPath();
        ExpectedRequest expectedRequest = ExpectedRequestBuilder.expectRequest().withPath(path).build();
        assertThat(expectedRequest.applies(recordedRequest)).describedAs("The response was not sent to the correct path: expected '%s', but got '%s'", path, recordedRequest.getPath()).isTrue();
    }

    private Response makepost(AttributeQueryContainerDto attributeQueryContainerDto, URI uri) {
        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(attributeQueryContainerDto));
    }

    private long getTotalBackoffPeriod(int retries, Duration duration){
        return ((retries * (retries + 1)) / 2) * duration.toMilliseconds();
    }

    private Scenario getScenario(String scenarioName) {
        for( Scenario s: getAllScenarios()) {
            if (s.getName().equals(scenarioName)) return s;
        }
        throw new RuntimeException(format("Scenario not found: %s ", scenarioName));
    }

    private Element convertToElementAndValidate(AttributeQueryContainerDto attributeQueryContainerDto) {
        try {
            Element matchingServiceRequest;
            matchingServiceRequest = XmlUtils.convertToElement(attributeQueryContainerDto.getSamlRequest());
            return matchingServiceRequest;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new InvalidSamlRequestInAttributeQueryException("Attribute Query had invalid XML.", e);
        } catch (SamlTransformationErrorException e) {
            throw new InvalidSamlRequestInAttributeQueryException("Attribute Query had invalid Saml", e);
        }
    }
}
