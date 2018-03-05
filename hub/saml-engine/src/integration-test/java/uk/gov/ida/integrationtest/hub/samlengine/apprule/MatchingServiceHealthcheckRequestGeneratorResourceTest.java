package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class MatchingServiceHealthcheckRequestGeneratorResourceTest {
    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setup() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder
                .aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration)
                .build(MatchingServiceHealthcheckRequestGeneratorResourceTest.class.getSimpleName());
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
        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE);
        return client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(dto));

    }
}
