package uk.gov.ida.contracttest.consumer.hub.policy;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import helpers.JerseyClientConfigurationBuilder;
import httpstub.RecordedRequest;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.extensions.EidasAuthnContext;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.idp.test.AuthnResponseFactory;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static au.com.dius.pact.model.PactSpecVersion.V3;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.COUNTRY_SELECTED_STATE;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

@Ignore
public class EidasSessionResourceContractTest {
    private static final String POLICY_SERVICE = "policy";
    private static final String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";
    private static final String SAML_ENGINE_SERVICE = "saml-engine";
    private static final URI SAML_ENGINE_STUB_URI = URI.create("http://localhost:" + getRandomUnallocatedPort());
    // The port number becomes part of the pact file so this must be kept consistent with the corresponding provider test
    private static final String COUNTRY_ENTITY_ID = "http://localhost:40000/metadata/country";
    private static final String EIDAS_HUB_ENTITY_ID = "http://localhost/eidasHubMetadata";
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA256();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private static final String SAML_ATTRIBUTE_QUERY = "SAMLAttributeQuery";
    private static final String MATCHING_SERVICE_URI = "matchingServiceUri";
    private static final String ID = "id";
    private static final XmlObjectToBase64EncodedStringTransformer<XMLObject> XML_OBJECT_XML_OBJECT_TO_BASE_64_ENCODED_STRING_TRANSFORMER = new XmlObjectToBase64EncodedStringTransformer<>();
    private static final String DESTINATION = "http://localhost:50300" + Urls.FrontendUrls.SAML2_SSO_EIDAS_RESPONSE_ENDPOINT;
    private static EncryptedAssertion encryptedIdentityAssertion;
    private static String encryptedIdentityAssertionString;

    private static SamlAuthnResponseContainerDto samlAuthnResponseContainerDto;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyProxyStub = new SamlSoapProxyProxyStubRule();

    @ClassRule
    public static PolicyAppRule policy = new PolicyAppRule(
        ConfigOverride.config("samlSoapProxyUri", samlSoapProxyProxyStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("samlEngineUri", SAML_ENGINE_STUB_URI.toASCIIString()),
        ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("eidas", "true"));

    @Rule
    public PactProviderRuleMk2 samlEngineServiceRule =
        new PactProviderRuleMk2(SAML_ENGINE_SERVICE, SAML_ENGINE_STUB_URI.getHost(), SAML_ENGINE_STUB_URI.getPort(), V3, this);

    private static Client client;
    private static SessionId sessionId;

    private static final ImmutableList<EidasCountryDto> EIDAS_COUNTRIES = ImmutableList.of(
        new EidasCountryDto("http://netherlandsEnitity.nl", "NL", true),
        new EidasCountryDto("http://spainEnitity.es", "ES", true));

    @BeforeClass
    public static void beforeClass() throws Exception {
        IdaSamlBootstrap.bootstrap();
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment())
            .using(jerseyClientConfiguration)
            .build(EidasSessionResourceContractTest.class.getSimpleName());
        sessionId = SessionId.createNewSessionId();
        samlAuthnResponseContainerDto = createAuthnResponseSignedByKeyPair(sessionId, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY);
        encryptedIdentityAssertion = AssertionBuilder.anAssertion().withId(UUID.randomUUID().toString()).build();
        encryptedIdentityAssertionString = XML_OBJECT_XML_OBJECT_TO_BASE_64_ENCODED_STRING_TRANSFORMER.apply(encryptedIdentityAssertion);
    }

    @Before
    public void setUp() throws Exception {
        configStub.reset();
        configStub.setUpStubForMatchingServiceRequest(TEST_RP, TEST_RP_MS, true);
        configStub.setUpStubForLevelsOfAssurance(TEST_RP);
        configStub.setupStubForEidasEnabledForTransaction(TEST_RP, true);
        configStub.setupStubForEidasCountries(EIDAS_COUNTRIES);
        eventSinkStub.setupStubForLogging();
        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
    }

    @Pact(consumer = POLICY_SERVICE, provider = SAML_ENGINE_SERVICE)
    public RequestResponsePact shouldGetValidatedEidasAuthnResponseFromSamlEngine(PactDslWithProvider pact) {
        return pact
            .uponReceiving("A request to validate the Authn Response")
            .path(Urls.SamlEngineUrls.TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE).method("POST")
            .headers(singletonMap(CONTENT_TYPE, APPLICATION_JSON))
            .body(
                new PactDslJsonBody()
                    .stringValue("samlResponse", samlAuthnResponseContainerDto.getSamlResponse())
                    .stringValue("principalIPAddressAsSeenByHub", samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub())
                    .stringValue("matchingServiceEntityId", TEST_RP_MS)
                    .object("sessionId")
                    .stringValue("sessionId", sessionId.getSessionId())
                    .closeObject()
            )
            .willRespondWith()
            .status(Response.Status.OK.getStatusCode())
            .headers(singletonMap(CONTENT_TYPE, APPLICATION_JSON))
            .body(
                new PactDslJsonBody()
                    .stringValue("issuer", COUNTRY_ENTITY_ID)
                    .stringValue("persistentId", "UK/GB/12345")
                    .stringValue("status", "Success")
                    .nullValue("statusMessage")
                    .stringMatcher("encryptedIdentityAssertionBlob", ".+", encryptedIdentityAssertionString)
                    .stringValue("levelOfAssurance", "LEVEL_2")
            )

            .uponReceiving("A request to generate an attribute query request")
            .path(Urls.SamlEngineUrls.GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE).method("POST")
            .headers(singletonMap(CONTENT_TYPE, APPLICATION_JSON))
            .body(
                new PactDslJsonBody()
                    .stringMatcher("requestId", ".+", ID)
                    .stringValue("assertionConsumerServiceUri", "/default-service-index")
                    .stringValue("encryptedIdentityAssertion", encryptedIdentityAssertionString)
                    .stringValue("authnRequestIssuerEntityId", TEST_RP)
                    .stringValue("levelOfAssurance", "LEVEL_2")
                    .stringValue("matchingServiceAdapterUri", MATCHING_SERVICE_URI)
                    .stringValue("matchingServiceEntityId", TEST_RP_MS)
                    .timestamp("matchingServiceRequestTimeOut", "yyyy-MM-dd'T'HH:mm:ss.S'Z'")
                    .booleanValue("onboarding", true)
                    .nullValue("cycle3Dataset")
                    .nullValue("userAccountCreationAttributes")
                    .timestamp("assertionExpiry", "yyyy-MM-dd'T'HH:mm:ss.S'Z'")
                    .object("persistentId")
                    .stringValue("nameId", "UK/GB/12345")
                    .closeObject()
            )
            .willRespondWith()
            .status(Response.Status.OK.getStatusCode())
            .headers(singletonMap(CONTENT_TYPE, APPLICATION_JSON))
            .body(
                new PactDslJsonBody()
                    .stringMatcher("samlRequest", "[\\s\\S]+", SAML_ATTRIBUTE_QUERY)
                    .stringValue("matchingServiceUri", MATCHING_SERVICE_URI)
                    .stringValue("id", ID)
                    .stringValue("issuer", EIDAS_HUB_ENTITY_ID)
                    .timestamp("attributeQueryClientTimeOut", "yyyy-MM-dd'T'HH:mm:ss.S'Z'")
                    .booleanValue("onboarding", true)
            )
            .toPact();
    }

    @PactVerification(value = SAML_ENGINE_SERVICE, fragment = "shouldGetValidatedEidasAuthnResponseFromSamlEngine")
    @Test
    public void shouldGetValidatedEidasAuthnResponseFromSamlEngineVerification() throws Exception {
        configStub.setupStubForEidasRPCountries(TEST_RP, EIDAS_COUNTRIES.stream().map(EidasCountryDto::getEntityId).collect(Collectors.toList()));
        SessionId sessionId = createSessionInCountrySelectingState();
        selectACountry(sessionId);

        Response response = postAuthnResponseToPolicy(sessionId);

        assertThatResponseIsSuccess(response);
        assertThatAQRReceivedBySamlSoapProxyHasSameDataAsSamlEngineSent();
    }

    private void assertThatResponseIsSuccess(Response response) {
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseAction expectedResult = ResponseAction.success(sessionId, false, LevelOfAssurance.LEVEL_2);
        assertThat(response.readEntity(ResponseAction.class)).isEqualToComparingFieldByField(expectedResult);
    }

    private void assertThatAQRReceivedBySamlSoapProxyHasSameDataAsSamlEngineSent() throws Exception {
        RecordedRequest request = samlSoapProxyProxyStub.getLastRequest();
        assertThat(samlSoapProxyProxyStub.getCountOfRequests()).isEqualTo(1);
        assertThat(request.getPath()).isEqualTo(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_REQUEST_SENDER_RESOURCE);
        String unzipped = IOUtils.toString(new GZIPInputStream(new ByteArrayInputStream(request.getEntityBytes())));
        AttributeQueryRequest aqr = policy.getObjectMapper().readValue(unzipped, AttributeQueryRequest.class);
        assertThat(aqr.getSamlRequest()).isEqualTo(SAML_ATTRIBUTE_QUERY);
        assertThat(aqr.getMatchingServiceUri().toASCIIString()).isEqualTo(MATCHING_SERVICE_URI);
        assertThat(aqr.getId()).isEqualTo(ID);
        assertThat(aqr.getIssuer()).isEqualTo(EIDAS_HUB_ENTITY_ID);
        assertThat(aqr.isOnboarding()).isTrue();
    }

    private SessionId createSessionInCountrySelectingState() {
        URI uri = policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + COUNTRY_SELECTED_STATE).build().toASCIIString());
        TestSessionResourceHelper.createSessionInCountrySelectingState(
            sessionId,
            client,
            uri,
            TEST_RP,
            true);
        return sessionId;
    }

    private Response postAuthnResponseToPolicy(SessionId sessionId) {
        URI countryResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.COUNTRY_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        return client
            .target(policy.uri(countryResponseUri.toASCIIString()))
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(samlAuthnResponseContainerDto));
    }

    private Response selectACountry(SessionId sessionId) {
        return TestSessionResourceHelper.selectCountryInSession(
            sessionId,
            client,
            policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE)
                .path(Urls.PolicyUrls.COUNTRY_SET_PATH)
                .build(sessionId, "NL").toString())
        );
    }

    private static int getRandomUnallocatedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static SamlAuthnResponseContainerDto createAuthnResponseSignedByKeyPair(SessionId sessionId, String publicKey, String privateKey) throws Exception {
        AuthnResponseFactory authnResponseFactory = AuthnResponseFactory.anAuthnResponseFactory();
        String samlResponse = authnResponseFactory.aSamlResponseFromCountry("a-request",
            COUNTRY_ENTITY_ID,
            publicKey,
            privateKey,
            DESTINATION,
            SIGNATURE_ALGORITHM,
            DIGEST_ALGORITHM,
            EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
            EidasAuthnContext.EIDAS_LOA_SUBSTANTIAL,
            DESTINATION,
            COUNTRY_ENTITY_ID);
        return new SamlAuthnResponseContainerDto(samlResponse, sessionId, "127.0.0.1");
    }
}
