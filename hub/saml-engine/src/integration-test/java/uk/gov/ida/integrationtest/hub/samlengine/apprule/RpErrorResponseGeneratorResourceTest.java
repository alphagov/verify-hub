package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.RequestForErrorResponseFromHubDtoBuilder.aRequestForErrorResponseFromHubDto;

public class RpErrorResponseGeneratorResourceTest {

    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(RpErrorResponseGeneratorResourceTest.class.getSimpleName());
    }

    @Before
    public void before() {
        DateTimeFreezer.freezeTime();
    }

    @After
    public void after() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldGenerateAnErrorResponseForAnRp() throws JsonProcessingException {
        RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto = aRequestForErrorResponseFromHubDto().build();
        configStub.setUpStubForShouldHubSignResponseMessagesForSamlStandard(requestForErrorResponseFromHubDto.getAuthnRequestIssuerEntityId());

        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE);
        Response rpAuthnResponse = post(requestForErrorResponseFromHubDto, uri);

        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        SamlMessageDto samlMessageDto = rpAuthnResponse.readEntity(SamlMessageDto.class);
        assertThat(samlMessageDto.getSamlMessage()).isNotNull();
    }

    @Test
    public void shouldReturnAnErrorResponseGivenBadInput() throws JsonProcessingException {
        RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto = aRequestForErrorResponseFromHubDto().withStatus(null).build();
        configStub.setUpStubForShouldHubSignResponseMessagesForSamlStandard(requestForErrorResponseFromHubDto.getAuthnRequestIssuerEntityId());

        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE);
        Response rpAuthnResponse = post(requestForErrorResponseFromHubDto, uri);

        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = rpAuthnResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }

    private Response post(RequestForErrorResponseFromHubDto dto, URI uri) {
        return client.target(uri).request().post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE));
    }
}
