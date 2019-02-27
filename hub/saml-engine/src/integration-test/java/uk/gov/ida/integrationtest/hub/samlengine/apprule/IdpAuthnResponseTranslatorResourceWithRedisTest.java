package uk.gov.ida.integrationtest.hub.samlengine.apprule;

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
import org.junit.rules.RuleChain;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.RedisTestRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.integrationtest.hub.samlengine.builders.AuthnResponseFactory;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.SamlAuthnResponseTranslatorDtoBuilder.aSamlAuthnResponseTranslatorDto;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_THREE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_TWO;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class IdpAuthnResponseTranslatorResourceWithRedisTest {

    private static final int REDIS_PORT = 6380;
    private static Client client;

    private final String IDP_RESPONSE_ENDPOINT = "http://localhost" + Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT;
    private final AuthnResponseFactory authnResponseFactory = new AuthnResponseFactory();

    private static RedisTestRule redis = new RedisTestRule(REDIS_PORT);

    private static ConfigStubRule configStubRule = new ConfigStubRule();

    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            ConfigOverride.config("configUri", configStubRule.baseUri().build().toASCIIString()),
            ConfigOverride.config("redis.uri", "redis://localhost:" + REDIS_PORT),
            ConfigOverride.config("redis.recordTTL", "PT150m")
    );

    @ClassRule
    public static RuleChain ruleChain = RuleChain.outerRule(redis).around(configStubRule).around(samlEngineAppRule);

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build
                (IdpAuthnRequestGeneratorResourceTest.class.getSimpleName());
    }

    @Before
    public void beforeEach() throws Exception {
        configStubRule.setupCertificatesForEntity(TEST_RP_MS);
    }

    @After
    public void after() {
        configStubRule.reset();
        DateTimeFreezer.unfreezeTime();
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
        return client.target(samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE))
                .request().post(Entity.entity(samlResponseDto, MediaType.APPLICATION_JSON_TYPE));
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
