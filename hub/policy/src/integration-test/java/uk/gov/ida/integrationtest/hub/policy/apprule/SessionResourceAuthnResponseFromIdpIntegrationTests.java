package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.PolicyApplication;
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
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubExtension;
import uk.gov.ida.integrationtest.hub.policy.builders.InboundResponseFromIdpDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.builders.SamlAuthnResponseContainerDtoBuilder;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionResourceAuthnResponseFromIdpIntegrationTests {

    private static final String THE_TRANSACTION_ID = "the-transaction-id";
    private static final boolean REGISTERING = true;
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;
    private static final String abTestVariant = null;
    private static ClientSupport client;

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
    public static TestDropwizardAppExtension policyApp = PolicyAppExtension.forApp(PolicyApplication.class)
            .withDefaultConfigOverridesAnd()
            .configOverride("samlEngineUri", () -> samlEngineStub.baseUri().build().toASCIIString())
            .configOverride("samlSoapProxyUri", () -> samlSoapProxyStub.baseUri().build().toASCIIString())
            .configOverride("configUri", () -> configStub.baseUri().build().toASCIIString())
            .configOverride("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("policy.yml"))
            .randomPorts()
            .create();

    private final String matchingServiceEntityId = "matchingServiceEntityId";
    private String idpEntityId = "Idp";
    private URI idpSsoUri = UriBuilder.fromPath("idpSsoUri").build();
    private SamlResponseWithAuthnRequestInformationDto samlResponse;
    private SamlAuthnRequestContainerDto samlRequest;
    private SessionId sessionId;
    private SamlAuthnResponseContainerDto samlResponseDto;

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
    }

    @BeforeEach
    public void setUp() throws Exception {
        samlResponse = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(THE_TRANSACTION_ID).build();
        samlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        configStub.setupStubForEnabledIdps(THE_TRANSACTION_ID, REGISTERING, REQUESTED_LOA, List.of(idpEntityId, "differentIdp"));
        configStub.setUpStubForLevelsOfAssurance(samlResponse.getIssuer());
        eventSinkStub.setupStubForLogging();
        configStub.setUpStubForMatchingServiceRequest(samlResponse.getIssuer(), matchingServiceEntityId);
        sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anAuthnRequestHasBeenSentToAnIdp(sessionId);
        samlResponseDto = SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto()
                                                              .withSamlResponse("a-saml-response")
                                                              .withSessionId(new SessionId(sessionId.getSessionId()))
                                                              .withPrincipalIPAddressAsSeenByHub("principal-ip-address")
                                                              .withAnalyticsSessionId("this-is-an-analytics-session-id")
                                                              .withJourneyType("this-is-a-journey-type")
                                                              .build();
    }

    @AfterEach
    public void resetStubs() {
        configStub.reset();
        eventSinkStub.reset();
        samlSoapProxyStub.reset();
        samlEngineStub.reset();
    }

    @AfterAll
    public static void tearDown() {
        PolicyAppExtension.tearDown();
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
    public void responsePost_shouldHandAuthnSuccessResponse(ObjectMapper mapper) throws Exception {
        LevelOfAssurance loaAchieved = LevelOfAssurance.LEVEL_2;
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDtoBuilder.successResponse(idpEntityId, loaAchieved));
        samlEngineStub.setupStubForAttributeQueryRequest(AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto().build());
        samlSoapProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        Response response = postIdpResponse(sessionId, samlResponseDto);
        ResponseAction expected = ResponseAction.success(sessionId, true, loaAchieved, null);
        ResponseAction actualResponseAction = response.readEntity(ResponseAction.class);
        assertThat(actualResponseAction).isEqualToComparingFieldByField(expected);

        SamlAuthnResponseTranslatorDto samlAuthnResponseTranslatorDto = samlEngineStub.getSamlAuthnResponseTranslatorDto(mapper);
        assertThat(samlAuthnResponseTranslatorDto.getMatchingServiceEntityId()).isEqualTo(matchingServiceEntityId);
    }

    @Test
    public void responsePost_shouldReturnBadRequestWhenIdpIsDifferentThanSelectedIdp() throws JsonProcessingException {
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
    public void responsePost_shouldReturnBadRequestSessionDoesNotExist() {
        Response response = postIdpResponse(SessionId.createNewSessionId(), samlResponseDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto responseEntity = response.readEntity(ErrorStatusDto.class);
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.SESSION_NOT_FOUND);
    }
    private Response postIdpResponse(SessionId sessionId, SamlAuthnResponseContainerDto samlResponseDto) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        return client
                .targetMain(uri.toASCIIString()).request()
                .post(Entity.entity(samlResponseDto, MediaType.APPLICATION_JSON_TYPE));
    }

    private void anIdpIsSelectedForRegistration(SessionId sessionId, String idpEntityId) {
        final URI policyUri = UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId);

        client.targetMain(policyUri.toASCIIString()).request()
                .post(Entity.entity(new IdpSelected(idpEntityId, "this-is-an-ip-address", REGISTERING, REQUESTED_LOA, "this-is-an-analytics-session-id", "this-is-a-journey-type", abTestVariant), MediaType
                        .APPLICATION_JSON_TYPE));
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(samlResponse.getIssuer());
        samlEngineStub.setupStubForAuthnRequestTranslate(samlResponse);
        return createASession(samlRequest).readEntity(SessionId.class);
    }

    public Response createASession(SamlAuthnRequestContainerDto samlRequest) {
        return client.targetMain(Urls.PolicyUrls.NEW_SESSION_RESOURCE)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(samlRequest));
    }

    private void anAuthnRequestHasBeenSentToAnIdp(SessionId sessionId) throws JsonProcessingException {
        final SamlRequestDto samlRequestDto = new SamlRequestDto("coffee-pasta", idpSsoUri);

        samlEngineStub.setupStubForIdpAuthnRequestGenerate(samlRequestDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_REQUEST_RESOURCE).build(sessionId);
        client.targetMain(uri.toASCIIString()).request()
                .get(Response.class);
    }
}
