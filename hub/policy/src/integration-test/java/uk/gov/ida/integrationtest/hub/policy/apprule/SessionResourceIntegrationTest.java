package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;
import uk.gov.ida.integrationtest.hub.policy.builders.InboundResponseFromIdpDtoBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.EIDAS_SUCCESSFUL_MATCH_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.IDP_SELECTED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.SUCCESSFUL_MATCH_STATE;
import static uk.gov.ida.integrationtest.hub.policy.builders.AuthnRequestFromHubContainerDtoBuilder.anAuthnRequestFromHubContainerDto;
import static uk.gov.ida.integrationtest.hub.policy.builders.AuthnResponseFromHubContainerDtoBuilder.anAuthnResponseFromHubContainerDto;
import static uk.gov.ida.integrationtest.hub.policy.builders.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;

public class SessionResourceIntegrationTest {
    private static final boolean REGISTERING = true;
    private static final boolean SIGNING_IN = !REGISTERING;
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;

    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";
    private static Client client;

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyProxyStub = new SamlSoapProxyProxyStubRule();

    @ClassRule
    public static PolicyAppRule policy = new PolicyAppRule(
            ConfigOverride.config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("samlSoapProxyUri", samlSoapProxyProxyStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()));

    private final String countryEntityId = "countryEntityId";
    private final String idpEntityId = "idpEntityId";
    private final String rpEntityId = "rpEntityId";
    private final URI idpSsoUri = UriBuilder.fromPath("idpSsoUri").build();
    private SamlResponseWithAuthnRequestInformationDto translatedAuthnRequest;
    private SamlAuthnRequestContainerDto rpSamlRequest;
    private String msEntityId = "Matching-service-entity-id";

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(SessionResourceIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(rpEntityId).build();
        rpSamlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        configStub.reset();
        configStub.setupStubForEnabledIdps(rpEntityId, REGISTERING, REQUESTED_LOA, singletonList(idpEntityId));
        configStub.setUpStubForEnabledCountries(rpEntityId, Collections.singletonList(new EidasCountryDto(countryEntityId, "simple-id", true)));
        configStub.setUpStubForLevelsOfAssurance(rpEntityId);
        configStub.setUpStubForMatchingServiceEntityId(rpEntityId, msEntityId);
        configStub.setupStubForEidasEnabledForTransaction(rpEntityId, true);
        eventSinkStub.setupStubForLogging();
    }

    @After
    public void tearDown() {
        configStub.reset();
        eventSinkStub.reset();
        samlEngineStub.reset();
    }

    @Test
    public void shouldCreateSession() throws Exception {
        configStub.setUpStubForAssertionConsumerServiceUri(rpEntityId);
        samlEngineStub.setupStubForAuthnRequestTranslate(translatedAuthnRequest);

        Response responseFromPost = createASession(rpSamlRequest);
        assertThat(responseFromPost.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());

        final SessionId postSessionId = responseFromPost.readEntity(SessionId.class);
        URI getUri = UriBuilder.fromUri(Urls.PolicyUrls.SESSION_RESOURCE_ROOT).path(Urls.SharedUrls.SESSION_ID_PARAM_PATH).build(postSessionId);
        final SessionId getSessionId = getEntity(getUri, SessionId.class);
        assertThat(postSessionId).isEqualTo(getSessionId);
        assertThat(eventSinkStub.getRecordedRequest()).hasSize(1);
        String eventSinkEntity = eventSinkStub.getRecordedRequest().get(0).getEntity();
        assertThat(eventSinkEntity).contains(postSessionId.getSessionId());
        assertThat(eventSinkEntity).contains(EventSinkHubEventConstants.SessionEvents.SESSION_STARTED);
        assertThat(eventSinkEntity).contains(rpSamlRequest.getPrincipalIPAddressAsSeenByHub());
    }

    @Test
    public void shouldReturnBadRequestWhenAssertionConsumerIndexIsInvalid() throws Exception {
        String missingRpEntityId = "other-entity-id";
        configStub.setUpStubToReturn404ForAssertionConsumerServiceUri(missingRpEntityId);
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(missingRpEntityId).build();
        samlEngineStub.setupStubForAuthnRequestTranslate(translatedAuthnRequest);

        checkException(createASession(rpSamlRequest), ExceptionType.INVALID_ASSERTION_CONSUMER_INDEX);
    }

    @Test
    public void shouldReturnInvalidSamlExceptionWhenSamlEngineThrowsInvalidSamlException() throws Exception {
        configStub.setUpStubForAssertionConsumerServiceUri(rpEntityId);
        samlEngineStub.setupStubToReturnInvalidSamlException();

        checkException(createASession(rpSamlRequest), ExceptionType.INVALID_SAML);
    }

    @Test
    public void getSessionShouldFailWhenSessionDoesNotExist() {
        SessionId invalidSessionId = SessionId.createNewSessionId();
        URI uri = UriBuilder.fromUri(Urls.PolicyUrls.SESSION_RESOURCE_ROOT).path(Urls.SharedUrls.SESSION_ID_PARAM_PATH).build(invalidSessionId);

        checkException(get(uri), ExceptionType.SESSION_NOT_FOUND);
    }

    @Test
    public void shouldReturnOkWhenGeneratingIdpAuthnRequestFromHubIsSuccessfulOnSignIn() throws Exception {
        // Given
        final SamlRequestDto samlRequestDto = new SamlRequestDto("coffee-pasta", idpSsoUri);

        samlEngineStub.setupStubForIdpAuthnRequestGenerate(samlRequestDto);
        configStub.setupStubForEnabledIdps(rpEntityId, false, REQUESTED_LOA, singletonList(idpEntityId));

        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForSignIn(sessionId, idpEntityId);

        final AuthnRequestFromHubContainerDto expectedResult = anAuthnRequestFromHubContainerDtoWithRegistering(samlRequestDto, false);

        // When
        AuthnRequestFromHubContainerDto result = getEntity(UriBuilder.fromPath(Urls.PolicyUrls
                .IDP_AUTHN_REQUEST_RESOURCE).build(sessionId), AuthnRequestFromHubContainerDto.class);

        //Then
        assertThat(result).isEqualToComparingFieldByField(expectedResult);
        IdpSelectedState sessionState = policy.getSessionState(sessionId, IdpSelectedState.class);
        assertThat(sessionState.getMatchingServiceEntityId()).isEqualTo(msEntityId);
    }

    @Test
    public void shouldReturnOkWhenGeneratingIdpAuthnRequestFromHubIsSuccessfulOnRegistration() throws Exception {
        // Given
        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);

        final SamlRequestDto samlRequestDto = new SamlRequestDto("coffee-pasta", idpSsoUri);
        final AuthnRequestFromHubContainerDto expectedResult = anAuthnRequestFromHubContainerDtoWithRegistering(samlRequestDto, true);

        samlEngineStub.setupStubForIdpAuthnRequestGenerate(samlRequestDto);

        // When
        AuthnRequestFromHubContainerDto result = getEntity(UriBuilder.fromPath(Urls.PolicyUrls
                .IDP_AUTHN_REQUEST_RESOURCE).build
                (sessionId), AuthnRequestFromHubContainerDto.class);

        //Then
        assertThat(result).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void shouldReturnNotFoundWhenSessionDoesNotExistInPolicy() throws Exception {
        // Given
        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForSignIn(sessionId, idpEntityId);

        // When
        Response response = get(UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_REQUEST_RESOURCE).build
                ("this-session-totally-does-not-exist"));

        //Then
        checkException(response, ExceptionType.SESSION_NOT_FOUND);
    }

    private void checkException(Response response, ExceptionType type) {
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.readEntity(ErrorStatusDto.class).getExceptionType()).isEqualTo(type);
    }

    private <T> T getEntity(URI uri, Class<T> type) {
        Response response = get(uri);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        return get(uri).readEntity(type);
    }

    private Response get(URI uri) {
        final URI uri1 = policy.uri(uri.toASCIIString());
        return client.target(uri1).request(MediaType.APPLICATION_JSON_TYPE).get();
    }

    @Test
    public void shouldGetRpResponseGivenASessionExistsInPolicy() throws JsonProcessingException {
        // Given
        SessionId sessionId = SessionId.createNewSessionId();
        configStub.setupStubForEnabledIdps(rpEntityId, SIGNING_IN, REQUESTED_LOA, singletonList(idpEntityId));
        Response sessionCreatedResponse = TestSessionResourceHelper.createSessionInSuccessfulMatchState(sessionId, rpEntityId, idpEntityId, client, policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + SUCCESSFUL_MATCH_STATE).build().toASCIIString()));
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto expectedAuthnResponseFromHub = anAuthnResponseFromHubContainerDto().build();
        samlEngineStub.setUpStubForAuthnResponseGenerate(expectedAuthnResponseFromHub);

        // When
        URI rpAuthResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.RP_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        Response responseForRp = client
                .target(policy.uri(rpAuthResponseUri.toASCIIString())).request().get();

        //Then
        assertThat(responseForRp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto authnResponseFromHub = responseForRp.readEntity
                (AuthnResponseFromHubContainerDto.class);
        assertThat(authnResponseFromHub).isEqualToComparingFieldByField(expectedAuthnResponseFromHub);
    }

    @Test
    public void shouldGetRpResponseGivenASessionInEidasSuccessfulMatchStateExists() throws JsonProcessingException {
        SessionId sessionId = SessionId.createNewSessionId();
        Response sessionCreatedResponse = TestSessionResourceHelper.createSessionInEidasSuccessfulMatchState(sessionId, rpEntityId, countryEntityId, client, policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + EIDAS_SUCCESSFUL_MATCH_STATE).build().toASCIIString()));
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto expectedAuthnResponseFromHub = anAuthnResponseFromHubContainerDto().build();
        samlEngineStub.setUpStubForAuthnResponseGenerate(expectedAuthnResponseFromHub);

        URI rpAuthResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.RP_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        Response responseForRp = client
                .target(policy.uri(rpAuthResponseUri.toASCIIString())).request().get();

        assertThat(responseForRp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto authnResponseFromHub = responseForRp.readEntity
                (AuthnResponseFromHubContainerDto.class);
        assertThat(authnResponseFromHub).isEqualToComparingFieldByField(expectedAuthnResponseFromHub);
    }

    @Test
    public void shouldUpdateSessionStateAndSendAnAttributeQueryRequestWhenASuccessResponseIsReceivedFromIdp() throws JsonProcessingException {
        // Given
        SessionId sessionId = SessionId.createNewSessionId();
        Response sessionCreatedResponse = TestSessionResourceHelper.createSessionInIdpSelectedState(sessionId, rpEntityId, idpEntityId, client,
                policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + IDP_SELECTED_STATE).build().toASCIIString()));
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        LevelOfAssurance loaAchieved = LevelOfAssurance.LEVEL_2;
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.successResponse(idpEntityId, loaAchieved));
        samlEngineStub.setupStubForAttributeQueryRequest(anAttributeQueryContainerDto().build());

        configStub.setUpStubForMatchingServiceRequest(rpEntityId, IdpSelectedStateBuilder.anIdpSelectedState().build().getMatchingServiceEntityId());

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);

        // When
        URI idpResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        Response response = client
                .target(policy.uri(idpResponseUri.toASCIIString()))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(aSamlAuthnResponseContainerDto().withSessionId(sessionId).build()));

        //Then
        ResponseAction expectedResult = ResponseAction.success(sessionId, true, loaAchieved);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseAction actualResult = response.readEntity(ResponseAction.class);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);

        assertThat(getSessionStateName(sessionId)).isEqualTo(Cycle0And1MatchRequestSentState.class.getName());
    }

    private String getSessionStateName(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + GET_SESSION_STATE_NAME).build(sessionId);

        Response response = client.target(policy.uri(uri.toASCIIString()))
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        return response.readEntity(String.class);

    }

    private AuthnRequestFromHubContainerDto anAuthnRequestFromHubContainerDtoWithRegistering(SamlRequestDto samlRequestDto, final boolean registering) {
        return anAuthnRequestFromHubContainerDto()
                .withSamlRequest(samlRequestDto.getSamlRequest())
                .withPostEndPoint(idpSsoUri)
                .withRegistering(registering)
                .build();
    }

    private void anIdpIsSelectedForRegistration(SessionId sessionId, String idpEntityId) {
        final URI policyUri = policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId).getPath());
        post(policyUri, new IdpSelected(idpEntityId, "this-is-an-ip-address", REGISTERING, REQUESTED_LOA));
    }

    private void anIdpIsSelectedForSignIn(SessionId sessionId, String idpEntityId) {
        final URI policyUri = policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId).getPath());

        client.target(policyUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new IdpSelected(idpEntityId, "this-is-an-ip-address", SIGNING_IN, REQUESTED_LOA)));
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(rpEntityId);
        samlEngineStub.setupStubForAuthnRequestTranslate(translatedAuthnRequest);
        return createASession(rpSamlRequest).readEntity(SessionId.class);
    }

    private Response createASession(SamlAuthnRequestContainerDto samlRequest) {
        return post(policy.uri(Urls.PolicyUrls.NEW_SESSION_RESOURCE), samlRequest);
    }

    private Response post(URI uri, Object entity) {
        return client.target(uri).request()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }
}
