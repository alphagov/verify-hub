package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineClient;

import javax.ws.rs.core.Response;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class MatchingServiceHealthcheckRequestGeneratorResourceTest {

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
    void setup() { client = samlEngineApp.getClient(); }

    @AfterAll
    public static void afterAll() {
        samlEngineApp.tearDown();
    }

    @Test
    public void should_createHealthcheckAttributeQueryRequest() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS);

        Response response = getAttributeQuery(new MatchingServiceHealthCheckerRequestDto(TEST_RP, TEST_RP_MS));
        SamlMessageDto entity = response.readEntity(SamlMessageDto.class);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(entity.getSamlMessage()).isNotNull();
    }

    @Test
    public void should_createHealthcheckAttributeQueryRequestShouldReturnErrorStatusDtoWhenThereIsAProblem() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS);

        Response response = getAttributeQuery(new MatchingServiceHealthCheckerRequestDto(TEST_RP, null));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }

    private Response getAttributeQuery(MatchingServiceHealthCheckerRequestDto dto) {
        return client.postTargetMain(Urls.SamlEngineUrls.GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE, dto);
    }
}
