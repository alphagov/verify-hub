package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineClient;
import uk.gov.ida.integrationtest.hub.samlengine.builders.AuthnResponseFactory;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.SamlAuthnResponseTranslatorDtoBuilder.aSamlAuthnResponseTranslatorDto;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_THREE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_TWO;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class IdpAuthnResponseTranslatorResourceWithRedisTest {

    private final String IDP_RESPONSE_ENDPOINT = "http://localhost" + Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT;
    private final AuthnResponseFactory authnResponseFactory = new AuthnResponseFactory();

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(1)
    @RegisterExtension
    public static SamlEngineAppExtension samlEngineApp = new SamlEngineAppExtensionBuilder()
            .withConfigOverrides(
                    config("redis.recordTTL", "PT150m"),
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
}
