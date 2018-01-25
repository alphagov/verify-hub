package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.builders.InboundResponseFromIdpDtoBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.UUID;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

public class SessionResourceAuthnResponseFromIdpIntegrationTests {

    public static final String THE_TRANSACTION_ID = "the-transaction-id";
    private static final boolean REGISTERING = true;
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
            config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
            config("samlSoapProxyUri", samlSoapProxyStub.baseUri().build().toASCIIString()),
            config("configUri", configStub.baseUri().build().toASCIIString()),
            config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()));
    private final String matchingServiceEntityId = "matchingServiceEntityId";

    private String idpEntityId = "Idp";
    private URI idpSsoUri = UriBuilder.fromPath("idpSsoUri").build();
    private SamlResponseWithAuthnRequestInformationDto samlResponse;
    private SamlAuthnRequestContainerDto samlRequest;
    private SessionId sessionId;
    private SamlAuthnResponseContainerDto samlResponseDto;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(SessionResourceAuthnResponseFromIdpIntegrationTests.class.getSimpleName());
    }

    @After
    public void resetStubs() {
        configStub.reset();
        eventSinkStub.reset();
        samlSoapProxyStub.reset();
        samlEngineStub.reset();
    }

    @Before
    public void setUp() throws Exception {
        samlResponse = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(THE_TRANSACTION_ID).build();
        samlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        configStub.setupStubForEnabledIdps(ImmutableList.of(idpEntityId, "differentIdp"));
        configStub.setUpStubForLevelsOfAssurance(samlResponse.getIssuer());
        configStub.setupStubForEidasEnabledForTransaction(THE_TRANSACTION_ID, false);
        eventSinkStub.setupStubForLogging();
        configStub.setUpStubForMatchingServiceRequest(samlResponse.getIssuer(), matchingServiceEntityId);
        sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anAuthnRequestHasBeenSentToAnIdp(sessionId);
        samlResponseDto = new SamlAuthnResponseContainerDto("a-saml-response", sessionId, "an-ip-address");
    }

    @Test
    public void responsePost_shouldHandleErrorResponseFromSamlEngine() throws Exception {
        samlEngineStub.setupStubForIdpAuthnResponseTranslateReturningError(ErrorStatusDto.createUnauditedErrorStatus(UUID.randomUUID(), ExceptionType.INVALID_SAML));

        Response response = postIdpResponse(sessionId, samlResponseDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto responseEntity = response.readEntity(ErrorStatusDto.class);
        assertThat(responseEntity.getExceptionType()).isEqualTo(ErrorStatusDto.createUnauditedErrorStatus(UUID.randomUUID(), ExceptionType.INVALID_SAML).getExceptionType());
    }

    @Test
    public void responsePost_shouldHandleRequesterErrorResponse() throws Exception {
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.errorResponse
                (idpEntityId, IdpIdaStatus.Status.RequesterError));

        Response response = postIdpResponse(sessionId, samlResponseDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseAction expected = ResponseAction.other(sessionId, true);
        ResponseAction actualResponseAction = response.readEntity(ResponseAction.class);
        assertThat(actualResponseAction).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void responsePost_shouldHandleFraudResponse() throws Exception {
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.fraudResponse
                (idpEntityId));

        Response response = postIdpResponse(sessionId, samlResponseDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        ResponseAction expected = ResponseAction.other(sessionId, true);
        ResponseAction actualResponseAction = response.readEntity(ResponseAction.class);
        assertThat(actualResponseAction).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void responsePost_shouldHandleAuthnFailedResponse() throws Exception {
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.failedResponse
                (idpEntityId));

        Response response = postIdpResponse(sessionId, samlResponseDto);

        ResponseAction expected = ResponseAction.other(sessionId, true);
        ResponseAction actualResponseAction = response.readEntity(ResponseAction.class);
        assertThat(actualResponseAction).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void responsePost_shouldHandleNoAuthnContextResponse() throws Exception {
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.noAuthnContextResponse
                (idpEntityId));

        Response response = postIdpResponse(sessionId, samlResponseDto);
        ResponseAction expected = ResponseAction.other(sessionId, true);
        ResponseAction actualResponseAction = response.readEntity(ResponseAction.class);
        assertThat(actualResponseAction).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void responsePost_shouldHandAuthnSuccessResponse() throws Exception {
        LevelOfAssurance loaAchieved = LevelOfAssurance.LEVEL_2;
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.successResponse(idpEntityId, loaAchieved));
        samlEngineStub.setupStubForAttributeQueryRequest(AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto().build());
        samlSoapProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        Response response = postIdpResponse(sessionId, samlResponseDto);
        ResponseAction expected = ResponseAction.success(sessionId, true, loaAchieved);
        ResponseAction actualResponseAction = response.readEntity(ResponseAction.class);
        assertThat(actualResponseAction).isEqualToComparingFieldByField(expected);

        SamlAuthnResponseTranslatorDto samlAuthnResponseTranslatorDto = samlEngineStub.getSamlAuthnResponseTranslatorDto(policy.getObjectMapper());
        assertThat(samlAuthnResponseTranslatorDto.getMatchingServiceEntityId()).isEqualTo(matchingServiceEntityId);
    }

    @Test
    public void responePost_shouldReturnBadRequestWhenIdpIsDifferentThanSelectedIdp() throws JsonProcessingException {
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.successResponse("differentIdp", LevelOfAssurance.LEVEL_2));
        Response response = postIdpResponse(sessionId, samlResponseDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto responseEntity = response.readEntity(ErrorStatusDto.class);
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.STATE_PROCESSING_VALIDATION);
    }

    @Test
    public void responsePost_shouldReturnBadRequestWhenLevelOfAssuranceIsNotMet() throws JsonProcessingException{
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.successResponse(idpEntityId, LevelOfAssurance.LEVEL_3));
        Response response = postIdpResponse(sessionId, samlResponseDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto responseEntity = response.readEntity(ErrorStatusDto.class);
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.STATE_PROCESSING_VALIDATION);
    }

    @Test
    public void responsePost_shouldReturnForbiddenWhenIdpIsNotAvailable() throws JsonProcessingException{
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.successResponse("idpDoesNotExist", LevelOfAssurance.LEVEL_2));
        Response response = postIdpResponse(sessionId, samlResponseDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        ErrorStatusDto responseEntity = response.readEntity(ErrorStatusDto.class);
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.IDP_DISABLED);
    }


    @Test
    public void responsePost_shouldReturnBadRequestSessionDoesNotExist() throws JsonProcessingException{
        Response response = postIdpResponse(SessionId.createNewSessionId(), samlResponseDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto responseEntity = response.readEntity(ErrorStatusDto.class);
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.SESSION_NOT_FOUND);
    }
    private Response postIdpResponse(SessionId sessionId, SamlAuthnResponseContainerDto samlResponseDto) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        return client
                .target(policy.uri(uri.toASCIIString())).request()
                .post(Entity.entity(samlResponseDto, MediaType.APPLICATION_JSON_TYPE));
    }

    private void anIdpIsSelectedForRegistration(SessionId sessionId, String idpEntityId) {
        final URI policyUri = policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId).getPath());

        client.target(policyUri).request()
                .post(Entity.entity(new IdpSelected(idpEntityId, "this-is-an-ip-address", REGISTERING), MediaType
                        .APPLICATION_JSON_TYPE));
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(samlResponse.getIssuer());
        samlEngineStub.setupStubForAuthnRequestTranslate(samlResponse);
        return createASession(samlRequest).readEntity(SessionId.class);
    }

    public Response createASession(SamlAuthnRequestContainerDto samlRequest) {
        return client.target(policy.uri(Urls.PolicyUrls.NEW_SESSION_RESOURCE))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(samlRequest));
    }

    private void anAuthnRequestHasBeenSentToAnIdp(SessionId sessionId) throws JsonProcessingException {
        final SamlRequestDto samlRequestDto = new SamlRequestDto("coffee-pasta", idpSsoUri);

        samlEngineStub.setupStubForIdpAuthnRequestGenerate(samlRequestDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_REQUEST_RESOURCE).build(sessionId);
        client.target(policy.uri(uri.toASCIIString())).request()
                .get(Response.class);
    }

}
