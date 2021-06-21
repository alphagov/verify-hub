package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.RequestForErrorResponseFromHubDtoBuilder.aRequestForErrorResponseFromHubDto;

public class RpErrorResponseGeneratorResourceTest {
    private static ClientSupport client;

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(1)
    @RegisterExtension
    public static TestDropwizardAppExtension samlEngineApp = SamlEngineAppExtension.forApp(SamlEngineApplication.class)
            .withDefaultConfigOverridesAnd()
            .configOverride("configUri", () -> configStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("saml-engine.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
    }

    @BeforeEach
    public void before() {
        DateTimeFreezer.freezeTime();
    }

    @AfterEach
    public void after() {
        DateTimeFreezer.unfreezeTime();
    }

    @AfterAll
    public static void afterAll() {
        SamlEngineAppExtension.tearDown();
    }

    @Test
    public void shouldGenerateAnErrorResponseForAnRp() throws JsonProcessingException {
        RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto = aRequestForErrorResponseFromHubDto().build();
        configStub.signResponsesAndUseSamlStandard(requestForErrorResponseFromHubDto.getAuthnRequestIssuerEntityId());

        Response rpAuthnResponse = post(requestForErrorResponseFromHubDto, Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE);

        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        SamlMessageDto samlMessageDto = rpAuthnResponse.readEntity(SamlMessageDto.class);
        assertThat(samlMessageDto.getSamlMessage()).isNotNull();
    }

    @Test
    public void shouldReturnAnErrorResponseGivenBadInput() throws JsonProcessingException {
        RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto = aRequestForErrorResponseFromHubDto().withStatus(null).build();
        configStub.signResponsesAndUseSamlStandard(requestForErrorResponseFromHubDto.getAuthnRequestIssuerEntityId());

        Response rpAuthnResponse = post(requestForErrorResponseFromHubDto, Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE);

        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = rpAuthnResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }

    private Response post(RequestForErrorResponseFromHubDto dto, String uri) {
        return client.targetMain(uri).request().post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE));
    }
}
