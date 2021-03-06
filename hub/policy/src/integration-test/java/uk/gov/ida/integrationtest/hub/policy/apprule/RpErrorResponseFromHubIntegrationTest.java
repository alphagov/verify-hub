package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlMessageDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension.PolicyClient;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubExtension;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

public class RpErrorResponseFromHubIntegrationTest {

    @Order(0)
    @RegisterExtension
    public static SamlEngineStubExtension samlEngineStub = new SamlEngineStubExtension();
    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();
    @Order(0)
    @RegisterExtension
    public static EventSinkStubExtension eventSinkStub = new EventSinkStubExtension();
    @Order(1)
    @RegisterExtension
    public static final PolicyAppExtension policyApp = PolicyAppExtension.builder()
            .withConfigOverrides(
                    config("samlEngineUri", () -> samlEngineStub.baseUri().build().toASCIIString()),
                    config("configUri", () -> configStub.baseUri().build().toASCIIString()),
                    config("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            )
            .build();

    private String rpEntityId;
    private SamlResponseWithAuthnRequestInformationDto translatedAuthnRequest;
    private SamlAuthnRequestContainerDto rpSamlRequest;

    public PolicyClient client;

    @BeforeEach
    public void setUp() throws Exception {
        client = policyApp.getClient();
        rpEntityId = "rpEntityId";
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(rpEntityId).build();
        rpSamlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        eventSinkStub.setupStubForLogging();
        configStub.setUpStubForLevelsOfAssurance(rpEntityId);
    }

    @AfterAll
    public static void tearDown() {
        policyApp.tearDown();
    }

    @Test
    public void shouldGenerateAnErrorAuthnResponseFromHub() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        SamlMessageDto samlMessageDto = new SamlMessageDto("saml");
        samlEngineStub.setUpStubForErrorResponseGenerate(samlMessageDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RP_ERROR_RESPONSE_RESOURCE).build(sessionId.getSessionId());
        Response response = get(uri);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto authnResponseFromHubContainerDto = response.readEntity(AuthnResponseFromHubContainerDto.class);
        assertThat(authnResponseFromHubContainerDto.getSamlResponse()).isEqualTo(samlMessageDto.getSamlMessage());
    }

    @Test
    public void shouldReturnErrorStatusCodeWhenCallToSamlEngineGoesAwry() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        samlEngineStub.setUpStubForErrorResponseGenerateErrorOccurring();

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RP_ERROR_RESPONSE_RESOURCE).build(sessionId.getSessionId());
        Response response = get(uri);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = response.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(rpEntityId);
        samlEngineStub.setupStubForAuthnRequestTranslate(translatedAuthnRequest);
        return createASession(rpSamlRequest).readEntity(SessionId.class);
    }

    private Response createASession(SamlAuthnRequestContainerDto samlRequest) {
        return post(UriBuilder.fromPath(Urls.PolicyUrls.NEW_SESSION_RESOURCE).build(), samlRequest);
    }

    private Response post(URI uri, Object entity) {
        return client.postTargetMain(uri, entity);
    }

    private Response get(URI uri) {
        return client.getTargetMain(uri);
    }
}
