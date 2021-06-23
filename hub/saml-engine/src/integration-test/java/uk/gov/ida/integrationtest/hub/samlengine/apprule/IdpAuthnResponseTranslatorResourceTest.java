package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.security.credential.BasicCredential;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineClient;

import uk.gov.ida.integrationtest.hub.samlengine.builders.AuthnResponseFactory;
import uk.gov.ida.integrationtest.hub.samlengine.builders.SamlAuthnResponseTranslatorDtoBuilder;
import uk.gov.ida.saml.core.test.HardCodedKeyStore;
import uk.gov.ida.saml.core.test.builders.StatusBuilder;
import uk.gov.ida.saml.core.test.builders.StatusCodeBuilder;
import uk.gov.ida.saml.core.test.builders.StatusMessageBuilder;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.SamlAuthnResponseTranslatorDtoBuilder.aSamlAuthnResponseTranslatorDto;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_THREE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_TWO;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class IdpAuthnResponseTranslatorResourceTest {

    private final Status AUTHN_FAILED_STATUS = buildStatus(StatusCode.RESPONDER, StatusCode.AUTHN_FAILED);
    private final Status NO_AUTHN_CONTEXT_STATUS = buildStatus(StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT);
    private final Status REQUESTER_ERROR_STATUS = buildStatus(StatusCode.REQUESTER);
    private final Status REQUESTER_ERROR_DENIED_STATUS = buildStatus(StatusCode.REQUESTER, StatusCode.REQUEST_DENIED);
    private final String IDP_RESPONSE_ENDPOINT = "http://localhost" + Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT;
    private final AuthnResponseFactory authnResponseFactory = new AuthnResponseFactory();

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(1)
    @RegisterExtension
    public static SamlEngineAppExtension samlEngineApp = new SamlEngineAppExtensionBuilder()
            .withConfigOverrides(
                    config("configUri", () -> configStub.baseUri().build().toASCIIString())
            )
            .build();

    private SamlEngineClient client;

    @BeforeEach
    public void beforeEach() throws Exception {
        client = samlEngineApp.getClient();
        configStub.setupCertificatesForEntity(TEST_RP_MS);
    }

    @AfterEach
    public void after() {
        configStub.reset();
        DateTimeFreezer.unfreezeTime();
    }

    @AfterAll
    public static void afterAll() {
        samlEngineApp.tearDown();
    }

    @Test
    public void shouldThrowExceptionWhenAuthnResponseIsSignedByAnRp() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory
                .aResponseFromIdpBuilder(TEST_RP, "127.0.0.1")
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        final String saml = authnResponseFactory.transformResponseToSaml(samlResponse);

        SamlAuthnResponseTranslatorDto dto = new SamlAuthnResponseTranslatorDto(saml, SessionId.createNewSessionId(), "127.0.0.1", TEST_RP_MS);
        Response response = postToSamlEngine(dto, Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private Response postToSamlEngine(SamlAuthnResponseTranslatorDto dto, String uri) {
        return client.postTargetMain(uri, dto);
    }

    @Test
    public void shouldReturnOkWhenResponseIsSignedByAnIdp() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE, "127.0.0.1")
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        final String saml = authnResponseFactory.transformResponseToSaml(samlResponse);

        final SessionId sessionId = SessionId.createNewSessionId();

        SamlAuthnResponseTranslatorDto dto = new SamlAuthnResponseTranslatorDto(saml, sessionId, "127.0.0.1", TEST_RP_MS);
        Response response = postToSamlEngine(dto, Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void shouldTranslateASuccessfulIdpAuthnResponse() throws Exception {
        final String ipAddressAsSeenByIdp = "256.256.256.256";
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE, ipAddressAsSeenByIdp)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
                .withSamlResponse(saml)
                .withMatchingServiceEntityId(TEST_RP_MS)
                .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getStatus()).isEqualTo(IdpIdaStatus.Status.Success);
        assertThat(inboundResponseFromIdpDto.getIssuer()).isEqualTo(samlAuthnResponse.getIssuer().getValue());
        assertThat(inboundResponseFromIdpDto.getPrincipalIpAddressAsSeenByIdp().get()).isEqualTo(ipAddressAsSeenByIdp);
        assertThat(inboundResponseFromIdpDto.getFraudIndicator()).isNotPresent();
        assertThat(inboundResponseFromIdpDto.getIdpFraudEventId()).isNotPresent();

        // TODO consider checking the actual values of the fields below, rather than just their presence
        assertThat(inboundResponseFromIdpDto.getEncryptedAuthnAssertion().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getEncryptedMatchingDatasetAssertion().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getPersistentId().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getNotOnOrAfter().isPresent()).isTrue();
    }

    @Test
    public void shouldPreserveStatusMessageForRequesterError() throws Exception {
        final String statusMessage = "status-message";

        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .anAuthnFailedResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .withStatus(StatusBuilder.aStatus()
                        .withMessage(StatusMessageBuilder.aStatusMessage().withMessage(statusMessage).build())
                        .withStatusCode(
                                StatusCodeBuilder.aStatusCode()
                                        .withValue(StatusCode.REQUESTER)
                                        .build())
                        .build())
                .build();

        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
                .withSamlResponse(saml)
                .withMatchingServiceEntityId(TEST_RP_MS)
                .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getStatus()).isEqualTo(IdpIdaStatus.Status.RequesterError);
        assertThat(inboundResponseFromIdpDto.getStatusMessage().get()).isEqualTo(statusMessage);
    }

    @Test
    public void shouldTranslateAnAuthenticationFailedResponseFromIdp() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .anAuthnFailedResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .withStatus(AUTHN_FAILED_STATUS)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto().withSamlResponse(saml).withMatchingServiceEntityId("IGNOREME").build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getStatus()).isEqualTo(IdpIdaStatus.Status.AuthenticationFailed);
        assertThat(inboundResponseFromIdpDto.getIssuer()).isEqualTo(samlAuthnResponse.getIssuer().getValue());
        checkFieldsForUnsuccessfulResponseDTO(inboundResponseFromIdpDto);
    }

    @Test
    public void shouldTranslateANoAuthnContextResponseFromIdp() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .anAuthnFailedResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .withStatus(NO_AUTHN_CONTEXT_STATUS)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
                .withSamlResponse(saml)
                .withMatchingServiceEntityId(TEST_RP_MS)
                .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getStatus()).isEqualTo(IdpIdaStatus.Status.NoAuthenticationContext);
        assertThat(inboundResponseFromIdpDto.getIssuer()).isEqualTo(samlAuthnResponse.getIssuer().getValue());
        checkFieldsForUnsuccessfulResponseDTO(inboundResponseFromIdpDto);
    }

    @Test
    public void shouldTranslateARequesterErrorResponseFromIdp() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .anAuthnFailedResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .withStatus(REQUESTER_ERROR_STATUS)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto().withSamlResponse(saml).withMatchingServiceEntityId("IGNOREME").build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getStatus()).isEqualTo(IdpIdaStatus.Status.RequesterError);
        assertThat(inboundResponseFromIdpDto.getIssuer()).isEqualTo(samlAuthnResponse.getIssuer().getValue());
        checkFieldsForUnsuccessfulResponseDTO(inboundResponseFromIdpDto);
    }

    @Test
    public void shouldTranslateARequesterErrorDeniedResponseFromIdp() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .anAuthnFailedResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .withStatus(REQUESTER_ERROR_DENIED_STATUS)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
                .withSamlResponse(saml)
                .withMatchingServiceEntityId(TEST_RP_MS)
                .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getStatus()).isEqualTo(IdpIdaStatus.Status.RequesterError);
        assertThat(inboundResponseFromIdpDto.getIssuer()).isEqualTo(samlAuthnResponse.getIssuer().getValue());
        checkFieldsForUnsuccessfulResponseDTO(inboundResponseFromIdpDto);
    }

    @Test
    public void shouldTranslateAFraudResponseFromIdp() throws Exception {
        String persistentId = UUID.randomUUID().toString();
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aFraudResponseFromIdpBuilder(STUB_IDP_ONE, persistentId)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
                .withSamlResponse(saml)
                .withMatchingServiceEntityId(TEST_RP_MS)
                .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getStatus()).isEqualTo(IdpIdaStatus.Status.Success);
        assertThat(inboundResponseFromIdpDto.getIssuer()).isEqualTo(samlAuthnResponse.getIssuer().getValue());
        // TODO consider checking the values of the ones we've checked presence of below
        assertThat(inboundResponseFromIdpDto.getEncryptedAuthnAssertion().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getEncryptedMatchingDatasetAssertion().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getPersistentId().get()).isEqualTo(persistentId);
        assertThat(inboundResponseFromIdpDto.getLevelOfAssurance().get()).isEqualTo(LevelOfAssurance.LEVEL_X);
        assertThat(inboundResponseFromIdpDto.getPrincipalIpAddressAsSeenByIdp().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getFraudIndicator().isPresent()).isTrue();
        assertThat(inboundResponseFromIdpDto.getIdpFraudEventId().isPresent()).isTrue();
    }

    @Test
    public void shouldNotTranslateAnIncorrectIdpAuthnResponse() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .withStatus(AUTHN_FAILED_STATUS)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto().withSamlResponse(saml).withMatchingServiceEntityId("IGNOREME").build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldFailWhenASuccessIdpAuthnResponseDoesNotContainAnIpAddressAsSeenByIdp() throws Exception {
        final String ipAddressAsSeenByIdp = null;
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE, ipAddressAsSeenByIdp)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
                .withMatchingServiceEntityId(TEST_RP_MS)
                .withSamlResponse(saml)
                .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldRejectResponseWhenContainsInvalidStatusCodeCombination() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .withStatus(buildStatus(StatusCode.REQUESTER, StatusCode.AUTHN_FAILED))
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto().withSamlResponse(saml).withMatchingServiceEntityId("IGNOREME").build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldThrowExceptionWithTimeoutErrorStatusIfBearerSubjectTimeIsTooOld() throws Exception {
        DateTimeFreezer.freezeTime(DateTime.now().minusDays(1));
        SamlAuthnResponseTranslatorDto samlResponseDto = getSuccessSamlAuthnResponseTranslatorDto();
        DateTimeFreezer.unfreezeTime();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldThrowExceptionIfResponseContainsPartsWithMismatchedPids() throws Exception {
        final String ipAddressAsSeenByIdp = "256.256.256.256";
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
            .aResponseFromIdpBuilder(STUB_IDP_ONE, ipAddressAsSeenByIdp, "some-pid", "some-different-pid")
            .withDestination("http://localhost" + Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT)
            .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
            .withSamlResponse(saml)
            .withMatchingServiceEntityId(TEST_RP_MS)
            .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldThrowExceptionIfResponseContainsPartsWithMismatchedIssuers() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
            .aResponseFromIdpBuilderWithIssuers(STUB_IDP_ONE, STUB_IDP_ONE, STUB_IDP_TWO)
            .withDestination("http://localhost" + Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT)
            .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
            .withSamlResponse(saml)
            .withMatchingServiceEntityId(TEST_RP_MS)
            .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldThrowExceptionIfResponseContainsAuthnAssertionInResponseToValuesNotMatchingRequestId() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
            .aResponseFromIdpBuilderWithInResponseToValues(STUB_IDP_ONE, "default-request-id", "wrong-request-id", "default-request-id")
            .withDestination("http://localhost" + Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT)
            .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
            .withSamlResponse(saml)
            .withMatchingServiceEntityId(TEST_RP_MS)
            .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldThrowExceptionIfResponseContainsMdsAssertionInResponseToValuesNotMatchingRequestId() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
            .aResponseFromIdpBuilderWithInResponseToValues(STUB_IDP_ONE, "default-request-id", "default-request-id", "other-request-id")
            .withDestination("http://localhost" + Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT)
            .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        SamlAuthnResponseTranslatorDto samlResponseDto = aSamlAuthnResponseTranslatorDto()
            .withSamlResponse(saml)
            .withMatchingServiceEntityId(TEST_RP_MS)
            .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void handleResponseFromIdp_shouldThrowExceptionAuthnResponseIsReplayed() throws Exception {
        SamlAuthnResponseTranslatorDto samlResponseDto = getSuccessSamlAuthnResponseTranslatorDto();

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void handleResponseFromIdp_shouldThrowExceptionIfAuthnStatementAssertionIsReplayedInResponseFromIdp() throws Exception {
        String authnStatementAssertionId = "authnStatementAssertionId" + UUID.randomUUID().toString();
        String mdsStatementAssertionId = "mdsStatementAssertionId" + UUID.randomUUID().toString();
        SamlAuthnResponseTranslatorDto samlResponseDto_1 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_ONE, authnStatementAssertionId, mdsStatementAssertionId + "-1");
        SamlAuthnResponseTranslatorDto samlResponseDto_2 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_ONE, authnStatementAssertionId, mdsStatementAssertionId + "-2");

        Response clientResponse = postToSamlEngine(samlResponseDto_1);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        clientResponse = postToSamlEngine(samlResponseDto_2);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void handleResponseFromIdp_shouldThrowExceptionIfmdsAssertionIsReplayedInResponseFromIdp() throws Exception {
        String authnStatementAssertionId = "authnStatementAssertionId" + UUID.randomUUID().toString();
        String mdsStatementAssertionId = "mdsStatementAssertionId" + UUID.randomUUID().toString();
        SamlAuthnResponseTranslatorDto samlResponseDto_1 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_ONE, authnStatementAssertionId + "-1", mdsStatementAssertionId);
        SamlAuthnResponseTranslatorDto samlResponseDto_2 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_ONE, authnStatementAssertionId + "-2", mdsStatementAssertionId);

        Response clientResponse = postToSamlEngine(samlResponseDto_1);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        clientResponse = postToSamlEngine(samlResponseDto_2);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void handleResponseFromIdp_shouldThrowExceptionForSecondIdpIfTwoIdpsSubmitAnAuthnStatementAssertionWithTheSameId() throws Exception {
        String authnStatementAssertionId = "authnStatementAssertionId"+UUID.randomUUID().toString();
        String mdsStatementAssertionId = "mdsStatementAssertionId"+UUID.randomUUID().toString();
        SamlAuthnResponseTranslatorDto samlResponseDto_1 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_TWO, authnStatementAssertionId, mdsStatementAssertionId + "-1");
        SamlAuthnResponseTranslatorDto samlResponseDto_2 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_THREE, authnStatementAssertionId, mdsStatementAssertionId + "-2");
        Response clientResponse = postToSamlEngine(samlResponseDto_1);
        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        clientResponse = postToSamlEngine(samlResponseDto_2);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }


    @Test
    public void handleResponseFromIdp_shouldProcessSecondAssertionIfTwoAssertionsHaveTheSameIdButTheFirstAssertionHasExpired() throws Exception {
        String authnStatementAssertionId = "authnStatementAssertionId" + UUID.randomUUID().toString();
        String mdsStatementAssertionId = "mdsStatementAssertionId" + UUID.randomUUID().toString();

        DateTimeFreezer.freezeTime(DateTime.now().minusMinutes(30));
        SamlAuthnResponseTranslatorDto samlResponseDto_1 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_ONE, authnStatementAssertionId, mdsStatementAssertionId);
        Response clientResponse = postToSamlEngine(samlResponseDto_1);
        DateTimeFreezer.unfreezeTime();
        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        SamlAuthnResponseTranslatorDto samlResponseDto_2 = getSuccessSamlAuthnResponseTranslatorDto(STUB_IDP_ONE, authnStatementAssertionId, mdsStatementAssertionId);
        clientResponse = postToSamlEngine(samlResponseDto_2);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    }

    @Test
    public void handleResponseFromIdp_shouldDecryptAssertionEncryptedWithPrimaryEncryptionCertificates() throws Exception {
        BasicCredential primaryEncryptionKey = new BasicCredential(new HardCodedKeyStore(HUB_ENTITY_ID).getPrimaryEncryptionKeyForEntity(HUB_ENTITY_ID));

        SamlAuthnResponseTranslatorDto samlResponseDto = getSuccessSamlAuthnResponseTranslatorDto(primaryEncryptionKey);

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void handleResponseFromIdp_shouldDecryptAssertionEncryptedWithSecondaryEncryptionCertificates() throws Exception {
        BasicCredential secondaryEncryptionKey = new BasicCredential(new HardCodedKeyStore(HUB_ENTITY_ID).getSecondaryEncryptionKeyForEntity(HUB_ENTITY_ID));

        SamlAuthnResponseTranslatorDto samlResponseDto = getSuccessSamlAuthnResponseTranslatorDto(secondaryEncryptionKey);

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void handleResponseFromIdp_shouldNotDecryptAssertionEncryptedWithIncorrectEncryptionCertificates() throws Exception {
        BasicCredential incorrectEncryptionKey = new BasicCredential(new HardCodedKeyStore(HUB_ENTITY_ID).getPrimaryEncryptionKeyForEntity(TEST_RP));

        SamlAuthnResponseTranslatorDto samlResponseDto = getSuccessSamlAuthnResponseTranslatorDto(incorrectEncryptionKey);

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_FAILED_TO_DECRYPT);
    }

    @Test
    public void shouldEncryptTheMatchingDatasetAssertionWhenGivenMatchingServiceEntityId() throws Exception {
        BasicCredential primaryEncryptionKey = new BasicCredential(new HardCodedKeyStore(HUB_ENTITY_ID).getPrimaryEncryptionKeyForEntity(HUB_ENTITY_ID));

        SamlAuthnResponseTranslatorDto samlResponseDto = getSuccessSamlAuthnResponseTranslatorDto(primaryEncryptionKey);

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromIdpDto inboundResponseFromIdpDto = clientResponse.readEntity(InboundResponseFromIdpDto.class);
        assertThat(inboundResponseFromIdpDto.getEncryptedMatchingDatasetAssertion().isPresent()).isTrue();

    }

    @Test
    public void return422IfMSEntityIdNotPresent() throws Exception {

        SamlAuthnResponseTranslatorDto samlResponseDto =
                SamlAuthnResponseTranslatorDtoBuilder
                .aSamlAuthnResponseTranslatorDto()
                .withMatchingServiceEntityId(null)
                .build();

        Response clientResponse = postToSamlEngine(samlResponseDto);
        assertThat(clientResponse.getStatus()).isEqualTo(422);
    }

    private Response postToSamlEngine(SamlAuthnResponseTranslatorDto samlResponseDto) {
        return client.postTargetMain(Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE, samlResponseDto);
    }

    private SamlAuthnResponseTranslatorDto getSuccessSamlAuthnResponseTranslatorDto() throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        return aSamlAuthnResponseTranslatorDto().withSamlResponse(saml)
                .withMatchingServiceEntityId(TEST_RP_MS)
                .build();
    }

    private SamlAuthnResponseTranslatorDto getSuccessSamlAuthnResponseTranslatorDto(BasicCredential basicCredential) throws Exception {
        return getSuccessSamlAuthnResponseTranslatorDto(basicCredential, TEST_RP_MS);
    }

    private SamlAuthnResponseTranslatorDto getSuccessSamlAuthnResponseTranslatorDto(BasicCredential basicCredential, String matchingServiceEntityId) throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE, basicCredential)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        return aSamlAuthnResponseTranslatorDto()
                .withSamlResponse(saml)
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .build();
    }

    private SamlAuthnResponseTranslatorDto getSuccessSamlAuthnResponseTranslatorDto(String STUB_IDP_ONE, String authnStatementAssertionId, String mdsStatementAssertionId) throws Exception {
        final org.opensaml.saml.saml2.core.Response samlAuthnResponse = authnResponseFactory
                .aResponseFromIdpBuilder(STUB_IDP_ONE, authnStatementAssertionId, mdsStatementAssertionId)
                .withDestination(IDP_RESPONSE_ENDPOINT)
                .build();
        String saml = authnResponseFactory.transformResponseToSaml(samlAuthnResponse);
        return aSamlAuthnResponseTranslatorDto()
                .withSamlResponse(saml)
                .withMatchingServiceEntityId(TEST_RP_MS)
                .build();
    }

    private Status buildStatus(String uri) {
        return buildStatus(StatusCodeBuilder.aStatusCode().withValue(uri).build());
    }

    private Status buildStatus(String uri, String subStatusCode) {
        return buildStatus(buildStatusCode(uri, subStatusCode));
    }

    private StatusCode buildStatusCode(String uri, String subStatusCode) {
        return StatusCodeBuilder.aStatusCode().withValue(uri).withSubStatusCode(StatusCodeBuilder.aStatusCode()
                .withValue(subStatusCode).build()).build();
    }

    private Status buildStatus(StatusCode statusCode) {
        return StatusBuilder.aStatus().withStatusCode(statusCode).build();
    }

    private void checkFieldsForUnsuccessfulResponseDTO(InboundResponseFromIdpDto inboundResponseFromIdpDto) {
        assertThat(inboundResponseFromIdpDto.getEncryptedAuthnAssertion()).isNotPresent();
        assertThat(inboundResponseFromIdpDto.getEncryptedMatchingDatasetAssertion()).isNotPresent();
        assertThat(inboundResponseFromIdpDto.getPersistentId()).isNotPresent();
        assertThat(inboundResponseFromIdpDto.getLevelOfAssurance()).isNotPresent();
        assertThat(inboundResponseFromIdpDto.getPrincipalIpAddressAsSeenByIdp()).isNotPresent();
        assertThat(inboundResponseFromIdpDto.getFraudIndicator()).isNotPresent();
        assertThat(inboundResponseFromIdpDto.getIdpFraudEventId()).isNotPresent();
    }

}
