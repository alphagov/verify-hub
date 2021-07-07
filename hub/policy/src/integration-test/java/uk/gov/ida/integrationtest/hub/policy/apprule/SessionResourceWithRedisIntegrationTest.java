package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension.PolicyClient;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;
import uk.gov.ida.integrationtest.hub.policy.builders.InboundResponseFromIdpDtoBuilder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.IDP_SELECTED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.SUCCESSFUL_MATCH_STATE;
import static uk.gov.ida.integrationtest.hub.policy.builders.AuthnRequestFromHubContainerDtoBuilder.anAuthnRequestFromHubContainerDto;
import static uk.gov.ida.integrationtest.hub.policy.builders.AuthnResponseFromHubContainerDtoBuilder.anAuthnResponseFromHubContainerDto;
import static uk.gov.ida.integrationtest.hub.policy.builders.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;

public class SessionResourceWithRedisIntegrationTest {
    private static final boolean REGISTERING = true;
    private static final boolean SIGNING_IN = !REGISTERING;
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;
    private static final String abTestVariant = null;

    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";

    @Order(0)
    @RegisterExtension
    public static SamlEngineStubExtension samlEngineStub = new SamlEngineStubExtension();
    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();
    @Order(0)
    @RegisterExtension
    public static EventSinkStubExtension eventSinkStub = new EventSinkStubExtension();
    @Order(0)
    @RegisterExtension
    public static SamlSoapProxyProxyStubExtension samlSoapProxyStub = new SamlSoapProxyProxyStubExtension();
    @Order(1)
    @RegisterExtension
    public static final PolicyAppExtension policyApp = PolicyAppExtension.builder()
            .withConfigOverrides(
                    config("samlEngineUri", () -> samlEngineStub.baseUri().build().toASCIIString()),
                    config("samlSoapProxyUri", () -> samlSoapProxyStub.baseUri().build().toASCIIString()),
                    config("configUri", () -> configStub.baseUri().build().toASCIIString()),
                    config("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            )
            .build();

    private final String idpEntityId = "idpEntityId";
    private final String rpEntityId = "rpEntityId";
    private final URI idpSsoUri = UriBuilder.fromPath("idpSsoUri").build();
    private SamlResponseWithAuthnRequestInformationDto translatedAuthnRequest;
    private SamlAuthnRequestContainerDto rpSamlRequest;
    private String msEntityId = "Matching-service-entity-id";

    public PolicyClient client;

    @BeforeEach
    public void setUp() throws Exception {
        client = policyApp.getClient();
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(rpEntityId).build();
        rpSamlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        configStub.reset();
        configStub.setupStubForEnabledIdps(rpEntityId, REGISTERING, REQUESTED_LOA, singletonList(idpEntityId));
        configStub.setUpStubForLevelsOfAssurance(rpEntityId);
        configStub.setUpStubForMatchingServiceEntityId(rpEntityId, msEntityId);
        eventSinkStub.setupStubForLogging();
    }

    @AfterEach
    public void tearDown() {
        configStub.reset();
        eventSinkStub.reset();
        samlEngineStub.reset();
    }

    @AfterAll
    public static void tearDownAll() {
        policyApp.tearDown();
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
        String eventSinkEntity = new String(eventSinkStub.getRecordedRequest().get(0).getEntityBytes());
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
        configStub.setupStubForEnabledIdps(rpEntityId, false, REQUESTED_LOA, singletonList(idpEntityId), emptyList());

        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForSignIn(sessionId, idpEntityId);

        final AuthnRequestFromHubContainerDto expectedResult = anAuthnRequestFromHubContainerDtoWithRegistering(samlRequestDto, false);

        // When
        AuthnRequestFromHubContainerDto result = getEntity(UriBuilder.fromPath(Urls.PolicyUrls
                .IDP_AUTHN_REQUEST_RESOURCE).build(sessionId), AuthnRequestFromHubContainerDto.class);

        //Then
        assertThat(result).isEqualToComparingFieldByField(expectedResult);
        //IdpSelectedState sessionState = policy.getSessionState(sessionId, IdpSelectedState.class);
        //assertThat(sessionState.getIdpEntityId()).isEqualTo(idpEntityId);
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
        return client.getTargetMain(uri);
    }

    private Response post(URI uri, Object entity) {
        return client.postTargetMain(uri, entity);
    }

    @Test
    public void shouldGetRpResponseGivenASessionExistsInPolicy() throws JsonProcessingException {
        // Given
        SessionId sessionId = SessionId.createNewSessionId();
        configStub.setupStubForEnabledIdps(rpEntityId, SIGNING_IN, REQUESTED_LOA, singletonList(idpEntityId));
        Response sessionCreatedResponse = TestSessionResourceHelper.createSessionInSuccessfulMatchState(sessionId, rpEntityId, idpEntityId, client, UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + SUCCESSFUL_MATCH_STATE).build());
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto expectedAuthnResponseFromHub = anAuthnResponseFromHubContainerDto().build();
        samlEngineStub.setUpStubForAuthnResponseGenerate(expectedAuthnResponseFromHub);

        // When
        URI rpAuthResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.RP_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        Response responseForRp = get(rpAuthResponseUri);

        //Then
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
                UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + IDP_SELECTED_STATE).build());
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        LevelOfAssurance loaAchieved = LevelOfAssurance.LEVEL_2;
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.successResponse(idpEntityId, loaAchieved));
        samlEngineStub.setupStubForAttributeQueryRequest(anAttributeQueryContainerDto().build());

        configStub.setUpStubForMatchingServiceRequest(rpEntityId, msEntityId);

        samlSoapProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);

        // When
        URI idpResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        Response response = post(idpResponseUri, aSamlAuthnResponseContainerDto().withSessionId(sessionId).build());

        //Then
        ResponseAction expectedResult = ResponseAction.success(sessionId, true, loaAchieved, null);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseAction actualResult = response.readEntity(ResponseAction.class);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);

        assertThat(getSessionStateName(sessionId)).isEqualTo(Cycle0And1MatchRequestSentState.class.getName());
    }

    private String getSessionStateName(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + GET_SESSION_STATE_NAME).build(sessionId);
        return get(uri).readEntity(String.class);
    }

    private AuthnRequestFromHubContainerDto anAuthnRequestFromHubContainerDtoWithRegistering(SamlRequestDto samlRequestDto, final boolean registering) {
        return anAuthnRequestFromHubContainerDto()
                .withSamlRequest(samlRequestDto.getSamlRequest())
                .withPostEndPoint(idpSsoUri)
                .withRegistering(registering)
                .build();
    }

    private void anIdpIsSelectedForRegistration(SessionId sessionId, String idpEntityId) {
        final URI policyUri = UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId);
        post(policyUri, new IdpSelected(idpEntityId, "this-is-an-ip-address", REGISTERING, REQUESTED_LOA, "this-is-an-analytics-session-id", "this-is-a-journey-type", abTestVariant));
    }

    private void anIdpIsSelectedForSignIn(SessionId sessionId, String idpEntityId) {
        final URI policyUri = UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId);

        post(policyUri, new IdpSelected(idpEntityId, "this-is-an-ip-address", SIGNING_IN, REQUESTED_LOA, "this-is-an-analytics-session-id", "this-is-a-journey-type", abTestVariant));
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(rpEntityId);
        samlEngineStub.setupStubForAuthnRequestTranslate(translatedAuthnRequest);
        Response aSession = createASession(rpSamlRequest);
        return aSession.readEntity(SessionId.class);
    }

    private Response createASession(SamlAuthnRequestContainerDto samlRequest) {
        return post(UriBuilder.fromPath(Urls.PolicyUrls.NEW_SESSION_RESOURCE).build(), samlRequest);
    }
}
