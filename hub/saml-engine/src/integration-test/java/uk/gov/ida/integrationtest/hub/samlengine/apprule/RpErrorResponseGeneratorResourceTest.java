package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineClient;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.core.Response;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.RequestForErrorResponseFromHubDtoBuilder.aRequestForErrorResponseFromHubDto;

public class RpErrorResponseGeneratorResourceTest {

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
    public void before() {
        client = samlEngineApp.getClient();
        DateTimeFreezer.freezeTime();
    }

    @AfterEach
    public void after() {
        DateTimeFreezer.unfreezeTime();
    }

    @AfterAll
    public static void afterAll() {
        samlEngineApp.tearDown();
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
        return client.postTargetMain(uri, dto);
    }
}
