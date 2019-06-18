package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jersey.validation.ValidationErrorMessage;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.domain.IdpConfigDtoBuilder;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInDetailsDto;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.AUTHN_FAILED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.EIDAS_AUTHN_FAILED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.IDP_SELECTED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.SUCCESSFUL_MATCH_STATE;

public class AuthnRequestFromTransactionResourceIntegrationTest {
    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";
    private static final Boolean REGISTERING = TRUE;
    private static Client client;

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();
    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();
    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();
    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyStub = new SamlSoapProxyProxyStubRule();
    @ClassRule
    public static PolicyAppRule policy = new PolicyAppRule(
            ConfigOverride.config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("samlSoapProxyUri", samlSoapProxyStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()));


    private final String principalIpAddress = "principalIpAddress";
    private final String matchingServiceEntityId = "matchingServiceEntityId";
    private final String idpEntityId = "Idp";
    private final String transactionEntityId = "my-transaction-id";
    private SamlResponseWithAuthnRequestInformationDto samlResponse;
    private SamlAuthnRequestContainerDto samlRequest;
    private SessionId sessionId;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(AuthnRequestFromTransactionResourceIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        samlResponse = aSamlResponseWithAuthnRequestInformationDto().withIssuer(transactionEntityId).build();
        samlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();
        configStub.setupStubForEnabledIdps(transactionEntityId, REGISTERING, LEVEL_2, ImmutableList.of(idpEntityId, "differentIdp"));
        configStub.setupStubForEidasEnabledForTransaction(transactionEntityId, false);
        configStub.setUpStubForLevelsOfAssurance(samlResponse.getIssuer());
        eventSinkStub.setupStubForLogging();
        configStub.setUpStubForMatchingServiceRequest(samlResponse.getIssuer(), matchingServiceEntityId);
        configStub.setupStubForIdpConfig("idp-a", IdpConfigDtoBuilder.anIdpConfigDto().build());
        configStub.setupStubForIdpConfig("idp-b", IdpConfigDtoBuilder.anIdpConfigDto().build());
        configStub.setupStubForIdpConfig("idp-c", IdpConfigDtoBuilder.anIdpConfigDto().build());
    }

    @After
    public void resetStubs() {
        configStub.reset();
        eventSinkStub.reset();
        samlSoapProxyStub.reset();
        samlEngineStub.reset();
    }

    @Test
    public void badEntityResponseThrown_WhenMandatoryFieldsAreMissing() throws Exception {
        sessionId = aSessionIsCreated();
        Response response = postIdpSelected(new IdpSelected(null, null, null, null, null, null));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);

        ValidationErrorMessage msg = response.readEntity(ValidationErrorMessage.class);

        assertThat(msg.getErrors()).contains("selectedIdpEntityId may not be empty");
        assertThat(msg.getErrors()).contains("principalIpAddress may not be empty");
        assertThat(msg.getErrors()).contains("registration may not be null");
        assertThat(msg.getErrors()).contains("requestedLoa may not be null");
    }

    @Test
    public void selectIdp_shouldReturnSuccessResponseAndAudit() throws JsonProcessingException {
        sessionId = aSessionIsCreated();
        Response response = postIdpSelected(new IdpSelected(idpEntityId, principalIpAddress, REGISTERING, LEVEL_2, "this-is-an-analytics-session-id", "this-is-a-journey-type"));

        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(eventSinkStub.getRecordedRequest()).hasSize(2); // one session started event, one idp selected event

        String recordedEvent = new String(eventSinkStub.getLastRequest().getEntityBytes());
        assertThat(recordedEvent).contains(sessionId.getSessionId());
        assertThat(recordedEvent).contains(principalIpAddress);
        assertThat(recordedEvent).contains(EventSinkHubEventConstants.SessionEvents.IDP_SELECTED);
        assertThat(recordedEvent).contains(samlResponse.getId());
    }

    @Test
    public void idpSelected_shouldThrowIfIdpIsNotAvailable() throws JsonProcessingException {
        sessionId = aSessionIsCreated();

        IdpSelected idpSelected = new IdpSelected("does-not-exist", principalIpAddress, REGISTERING, LEVEL_2, "this-is-an-analytics-session-id", "this-is-a-journey-type");
        Response response = postIdpSelected(idpSelected);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto error = response.readEntity(ErrorStatusDto.class);
        assertThat(error.getExceptionType()).isEqualTo(ExceptionType.STATE_PROCESSING_VALIDATION);
    }

    @Test
    public void idpSelected_shouldThrowIfSessionInWrongState(){
        sessionId = SessionId.createNewSessionId();
        TestSessionResourceHelper.createSessionInSuccessfulMatchState(sessionId, transactionEntityId, idpEntityId, client, buildUriForTestSession(SUCCESSFUL_MATCH_STATE, sessionId));

        Response response = postIdpSelected(new IdpSelected("does-not-exist", principalIpAddress, REGISTERING, LEVEL_2, "this-is-an-analytics-session-id", "this-is-a-journey-type"));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto error = response.readEntity(ErrorStatusDto.class);
        assertThat(error.getExceptionType()).isEqualTo(ExceptionType.INVALID_STATE);
    }

    @Test
    public void tryAnotherIdp_shouldReturnSuccess(){
        sessionId = SessionId.createNewSessionId();
        TestSessionResourceHelper.createSessionInAuthnFailedErrorState(sessionId, client, buildUriForTestSession(AUTHN_FAILED_STATE, sessionId));

        URI uri = UriBuilder.fromPath("/policy/received-authn-request" + Urls.PolicyUrls.AUTHN_REQUEST_TRY_ANOTHER_IDP_PATH).build(sessionId);
        Response response = client.target(policy.uri(uri.toASCIIString())).request().post(null);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

        Response checkStateChanged = client.target(buildUriForTestSession(GET_SESSION_STATE_NAME, sessionId)).request().get();
        assertThat(checkStateChanged.readEntity(String.class)).isEqualTo(SessionStartedState.class.getName());
    }

    @Test
    public void shouldRestartIdpJourney() {
        sessionId = SessionId.createNewSessionId();
        TestSessionResourceHelper.createSessionInIdpSelectedState(sessionId, samlResponse.getIssuer(), idpEntityId, client, buildUriForTestSession(IDP_SELECTED_STATE, sessionId));

        URI uri = UriBuilder.fromPath("/policy/received-authn-request" + Urls.PolicyUrls.AUTHN_REQUEST_RESTART_JOURNEY_PATH).build(sessionId);
        Response response = client.target(policy.uri(uri.toASCIIString())).request().post(null);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

        Response checkStateChanged = client.target(buildUriForTestSession(GET_SESSION_STATE_NAME, sessionId)).request().get();
        assertThat(checkStateChanged.readEntity(String.class)).isEqualTo(SessionStartedState.class.getName());
    }

    @Test
    public void shouldRestartEidasJourney() {
        sessionId = SessionId.createNewSessionId();
        TestSessionResourceHelper.createSessionInEidasAuthnFailedErrorState(sessionId, client, buildUriForTestSession(EIDAS_AUTHN_FAILED_STATE, sessionId));

        URI uri = UriBuilder.fromPath("/policy/received-authn-request" + Urls.PolicyUrls.AUTHN_REQUEST_RESTART_JOURNEY_PATH).build(sessionId);
        Response response = client.target(policy.uri(uri.toASCIIString())).request().post(null);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

        Response checkStateChanged = client.target(buildUriForTestSession(GET_SESSION_STATE_NAME, sessionId)).request().get();
        assertThat(checkStateChanged.readEntity(String.class)).isEqualTo(SessionStartedState.class.getName());
    }

    @Test
    public void getSignInProcessDto_shouldReturnSignInDetailsDto(){
        SessionId session = SessionId.createNewSessionId();
        TestSessionResourceHelper.createSessionInIdpSelectedState(session, samlResponse.getIssuer(), idpEntityId, client,
                buildUriForTestSession(IDP_SELECTED_STATE, session));

        Response response = getAuthRequestSignInProcess(session);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnRequestSignInDetailsDto entity = response.readEntity(AuthnRequestSignInDetailsDto.class);
        assertThat(entity.getRequestIssuerId()).isEqualTo(samlResponse.getIssuer());
    }

    @Test
    public void getRequestIssuerId_shouldReturnRequestIssuerId(){
        SessionId session = SessionId.createNewSessionId();
        TestSessionResourceHelper.createSessionInIdpSelectedState(session, samlResponse.getIssuer(), idpEntityId, client,
                buildUriForTestSession(IDP_SELECTED_STATE, session));

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_FROM_TRANSACTION_ROOT + Urls.PolicyUrls.AUTHN_REQUEST_ISSUER_ID_PATH).build(session);
        Response response = client.target(policy.uri(uri.toASCIIString())).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(String.class)).isEqualTo(samlResponse.getIssuer());
    }

    private Response getAuthRequestSignInProcess(SessionId session) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SIGN_IN_PROCESS_RESOURCE).build(session);
        return client.target(policy.uri(uri.toASCIIString())).request().get();
    }

    private Response postIdpSelected(IdpSelected idpSelected){
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId);
        return client.target(policy.uri(uri.toASCIIString())).request()
                .post(Entity.entity(idpSelected, MediaType.APPLICATION_JSON_TYPE));
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(samlResponse.getIssuer());
        samlEngineStub.setupStubForAuthnRequestTranslate(samlResponse);
        return createASession(samlRequest).readEntity(SessionId.class);
    }

    private Response createASession(SamlAuthnRequestContainerDto samlRequest) {
        return client.target(policy.uri(Urls.PolicyUrls.NEW_SESSION_RESOURCE))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(samlRequest));
    }

    private URI buildUriForTestSession(String method, SessionId session) {
        return policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + method).build(session).toASCIIString());
    }
}
