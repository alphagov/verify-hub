package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
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
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRuleWithRedis;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class RpErrorResponseFromHubIntegrationTest {

    private static Client client;

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static PolicyAppRuleWithRedis policy = new PolicyAppRuleWithRedis(
            ConfigOverride.config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()));

    private String rpEntityId;
    private SamlResponseWithAuthnRequestInformationDto translatedAuthnRequest;
    private SamlAuthnRequestContainerDto rpSamlRequest;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(RpErrorResponseFromHubIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        rpEntityId = "rpEntityId";
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(rpEntityId).build();
        rpSamlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        eventSinkStub.setupStubForLogging();
        configStub.setUpStubForLevelsOfAssurance(rpEntityId);
        configStub.setupStubForEidasEnabledForTransaction(rpEntityId, false);
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
        return post(policy.uri(Urls.PolicyUrls.NEW_SESSION_RESOURCE), samlRequest);
    }

    private Response post(URI uri, Object entity) {
        return client.target(uri).request()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    private Response get(URI uri) {
        final URI uri1 = policy.uri(uri.toASCIIString());
        return client.target(uri1).request(MediaType.APPLICATION_JSON_TYPE).get();
    }
}
