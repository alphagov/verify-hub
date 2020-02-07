package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.Cycle3UserInput;
import uk.gov.ida.hub.policy.domain.MatchingProcessDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRuleWithRedis;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.rest.EidasCycle3DTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.domain.Cycle3AttributeRequestDataBuilder.aCycle3AttributeRequestData;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.EIDAS_AWAITING_CYCLE_3_DATA_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;

public class EidasCycle3DataResourceTest {
    public static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyProxyStub = new SamlSoapProxyProxyStubRule();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();


    @ClassRule
    public static PolicyAppRuleWithRedis policy = new PolicyAppRuleWithRedis(
            config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
            config("configUri", configStub.baseUri().build().toASCIIString()),
            config("samlSoapProxyUri", samlSoapProxyProxyStub.baseUri().build().toASCIIString()),
            config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
            config("eidas", "true"));

    private static Client client;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(EidasCycle3DataResourceTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        eventSinkStub.setupStubForLogging();
    }

    @Test
    public void shouldGetCycle3AttributeRequestDataFromConfiguration() throws JsonProcessingException {
        final SessionId sessionId = SessionIdBuilder.aSessionId().build();
        final String rpEntityId = new EidasCycle3DTO(sessionId).getRequestIssuerEntityId();
        final Response sessionCreatedResponse = createSessionInEidasAwaitingCycle3DataState(sessionId);
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final MatchingProcessDto cycle3Attribute = new MatchingProcessDto(Optional.of("TUFTY_CLUB_CARD"));
        configStub.setUpStubForEnteringAwaitingCycle3DataState(rpEntityId, cycle3Attribute);
        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);

        final Cycle3AttributeRequestData actualResponse = getCycle3Data(sessionId);

        final Cycle3AttributeRequestData expectedResponse = aCycle3AttributeRequestData()
            .withAttributeName(cycle3Attribute.getAttributeName().get())
            .withRequestIssuerId(rpEntityId)
            .build();
        assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse);
    }

    @Test
    public void shouldUpdateSessionStateToCancelledCycle3InputStateWhenInputToCycle3IsCancelled() {
        final SessionId sessionId = SessionIdBuilder.aSessionId().build();
        final Response sessionCreatedResponse = createSessionInEidasAwaitingCycle3DataState(sessionId);
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        cancelCycle3Data(sessionId);

        assertThat(getSessionStateName(sessionId)).isEqualTo(Cycle3DataInputCancelledState.class.getName());
    }

    @Test
    public void shouldReturnSuccessWhenDataSubmitted() throws JsonProcessingException {
        final SessionId sessionId = SessionIdBuilder.aSessionId().build();
        final String rpEntityId = new EidasCycle3DTO(sessionId).getRequestIssuerEntityId();
        final String msaEntityId = new EidasCycle3DTO(sessionId).getMatchingServiceAdapterEntityId();
        final Response sessionCreatedResponse = createSessionInEidasAwaitingCycle3DataState(sessionId);
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        final Cycle3UserInput cycle3UserInput = new Cycle3UserInput("test-value", "principal-ip-address-seen-by-hub");
        samlEngineStub.setupStubForEidasAttributeQueryRequestGeneration(AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto().build());
        configStub.setUpStubForMatchingServiceRequest(rpEntityId, msaEntityId);
        final MatchingProcessDto cycle3Attribute = new MatchingProcessDto(Optional.of("TUFTY_CLUB_CARD"));
        configStub.setUpStubForEnteringAwaitingCycle3DataState(rpEntityId, cycle3Attribute);
        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);

        postCycle3Data(sessionId, cycle3UserInput);

        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasCycle3MatchRequestSentState.class.getName());
    }

    private Cycle3AttributeRequestData getCycle3Data(final SessionId sessionId) {
        final URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_REQUEST_RESOURCE).build(sessionId);
        return client.target(policy.uri(uri.toASCIIString()))
            .request()
            .get()
            .readEntity(Cycle3AttributeRequestData.class);
    }

    private Response cancelCycle3Data(final SessionId sessionId) {
        final URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_CANCEL_RESOURCE).build(sessionId);
        return client.target(policy.uri(uri.toASCIIString()))
            .request()
            .post(null);
    }

    private Response postCycle3Data(final SessionId sessionId, final Cycle3UserInput data) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_SUBMIT_RESOURCE).build(sessionId);
        return client.target(policy.uri(uri.toASCIIString()))
            .request()
            .post(Entity.json(data));
    }

    private String getSessionStateName(final SessionId sessionId) {
        final URI uri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + GET_SESSION_STATE_NAME).build(sessionId);
        return client.target(policy.uri(uri.toASCIIString()))
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get()
            .readEntity(String.class);

    }

    private Response createSessionInEidasAwaitingCycle3DataState(final SessionId sessionId) {
        final URI uri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + EIDAS_AWAITING_CYCLE_3_DATA_STATE).build();
        final EidasCycle3DTO dto = new EidasCycle3DTO(sessionId);
        return client.target(policy.uri(uri.toASCIIString()))
            .request()
            .post(Entity.json(dto));
    }
}
