package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.Cycle3UserInput;
import uk.gov.ida.hub.policy.domain.MatchingProcessDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppExtension.PolicyClient;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubExtension;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubExtension;
import uk.gov.ida.integrationtest.hub.policy.rest.Cycle3DTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.domain.Cycle3AttributeRequestDataBuilder.aCycle3AttributeRequestData;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.AWAITING_CYCLE_3_DATA_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;

public class Cycle3DataResourceTest {
    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";

    @Order(0)
    @RegisterExtension
    public static SamlEngineStubExtension samlEngineStub = new SamlEngineStubExtension();
    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();
    @Order(0)
    @RegisterExtension
    public static EventSinkStubExtension eventSinkStub = new EventSinkStubExtension();
    @Order(0)
    @RegisterExtension
    public static SamlSoapProxyProxyStubExtension samlSoapProxyStub = new SamlSoapProxyProxyStubExtension();
    @Order(1)
    @RegisterExtension
    public static final PolicyAppExtension policyApp = PolicyAppExtension.builder()
            .withConfigOverrides(
                    config("samlEngineUri", () -> samlEngineStub.baseUri().build().toASCIIString()),
                    config("samlSoapProxyUri", () -> samlSoapProxyStub.baseUri().build().toASCIIString()),
                    config("configUri", () -> configStub.baseUri().build().toASCIIString()),
                    config("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            )
            .build();

    public PolicyClient client;

    @BeforeEach
    public void setUp() throws Exception {
        client = policyApp.getClient();
        eventSinkStub.setupStubForLogging();
    }

    @AfterAll
    public static void tearDown() {
        policyApp.tearDown();
    }

    @Test
    public void shouldReturnSuccessWhenDataSubmitted() throws JsonProcessingException {
        //Given
        SessionId sessionId = SessionIdBuilder.aSessionId().build();
        String rpEntityId = new Cycle3DTO(sessionId).getRequestIssuerId();
        String msaEntityId = new Cycle3DTO(sessionId).getMatchingServiceEntityId();
        Response sessionCreatedResponse = createSessionInAwaitingCycle3DataState(sessionId);
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final Cycle3UserInput cycle3UserInput = new Cycle3UserInput("test-value", "principal-ip-address-seen-by-hub");

        samlEngineStub.setupStubForAttributeQueryRequest(AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto().build());
        configStub.setUpStubForMatchingServiceRequest(rpEntityId, msaEntityId);
        final MatchingProcessDto cycle3Attribute = new MatchingProcessDto(Optional.of("TUFTY_CLUB_CARD"));
        configStub.setUpStubForEnteringAwaitingCycle3DataState(rpEntityId, cycle3Attribute);
        samlSoapProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);

        //When
        postCycle3Data(sessionId, cycle3UserInput);

        //Then
        assertThat(getSessionStateName(sessionId)).isEqualTo(Cycle3MatchRequestSentState.class.getName());
    }

    @Test
    public void shouldUpdateSessionStateToCancelledCycle3InputStateWhenInputToCycle3IsCancelled() {
        //Given
        SessionId sessionId = SessionIdBuilder.aSessionId().build();
        Response sessionCreatedResponse = createSessionInAwaitingCycle3DataState(sessionId);
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        //When
        cancelCycle3Data(sessionId);

        //Then
        assertThat(getSessionStateName(sessionId)).isEqualTo(Cycle3DataInputCancelledState.class.getName());
    }

    @Test
    public void shouldGetCycle3AttributeRequestDataFromConfiguration() throws JsonProcessingException {
        //Given
        SessionId sessionId = SessionIdBuilder.aSessionId().build();
        String rpEntityId = new Cycle3DTO(sessionId).getRequestIssuerId();
        Response sessionCreatedResponse = createSessionInAwaitingCycle3DataState(sessionId);
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        final MatchingProcessDto cycle3Attribute = new MatchingProcessDto(Optional.of("TUFTY_CLUB_CARD"));

        configStub.setUpStubForEnteringAwaitingCycle3DataState(rpEntityId, cycle3Attribute);
        samlSoapProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);

        //When
        Cycle3AttributeRequestData actualResponse = getCycle3Data(sessionId);

        //Then
        Cycle3AttributeRequestData expectedResponse = aCycle3AttributeRequestData()
                .withAttributeName(cycle3Attribute.getAttributeName().get())
                .withRequestIssuerId(rpEntityId).build();
        assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse);
    }

    private Cycle3AttributeRequestData getCycle3Data(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_REQUEST_RESOURCE).build(sessionId);
        Response response = client.getTargetMain(uri);
        return response.readEntity(Cycle3AttributeRequestData.class);
    }

    private String getSessionStateName(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + GET_SESSION_STATE_NAME).build(sessionId);

        Response response = client.getTargetMain(uri);
        return response.readEntity(String.class);
    }

    private Response postCycle3Data(SessionId sessionId, Cycle3UserInput data) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_SUBMIT_RESOURCE).build(sessionId);
        return client.postTargetMain(uri.toASCIIString(), data);
    }

    private Response cancelCycle3Data(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_CANCEL_RESOURCE).build(sessionId);
        return client.postTargetMain(uri, null);
    }

    private Response createSessionInAwaitingCycle3DataState(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + AWAITING_CYCLE_3_DATA_STATE).build();
        Cycle3DTO dto = new Cycle3DTO(sessionId);
        return client.postTargetMain(uri, dto);
    }
}
