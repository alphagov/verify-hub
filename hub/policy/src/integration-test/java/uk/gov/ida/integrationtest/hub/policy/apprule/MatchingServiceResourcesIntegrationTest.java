package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.Cycle3UserInput;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.policy.domain.MatchingServiceIdaStatus;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.builders.InboundResponseFromIdpDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.builders.SamlAuthnResponseContainerDtoBuilder;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;

public class MatchingServiceResourcesIntegrationTest {

    private static final String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";
    private static final boolean REGISTERING = true;

    private static Client client;

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyProxyStubRule = new SamlSoapProxyProxyStubRule();

    public static final int matchingServiceResponseWaitPeriodSeconds = 60;

    @ClassRule
    public static PolicyAppRule policy = new PolicyAppRule(
            config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
            config("configUri", configStub.baseUri().build().toASCIIString()),
            config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
            config("samlSoapProxyUri", samlSoapProxyProxyStubRule.baseUri().build().toASCIIString()),
            config("matchingServiceResponseWaitPeriod", format("{0}s", matchingServiceResponseWaitPeriodSeconds))
    );

    private String idpEntityId;
    private String rpEntityId;
    private String msaEntityId;
    private URI idpSsoUri;
    private SamlResponseWithAuthnRequestInformationDto translatedAuthnRequest;
    private SamlAuthnRequestContainerDto rpSamlRequest;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(MatchingServiceResourcesIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        idpEntityId = "idpEntityId";
        rpEntityId = "rpEntityId";
        msaEntityId = "msaEntityId";
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(rpEntityId).build();
        rpSamlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();
        idpSsoUri = UriBuilder.fromPath("idpSsoUri").build();
        
        configStub.reset();
        configStub.setupStubForEnabledIdps(rpEntityId, REGISTERING, LEVEL_2, singletonList(idpEntityId));
        configStub.setUpStubForLevelsOfAssurance(rpEntityId);
        configStub.setUpStubForMatchingServiceEntityId(rpEntityId, msaEntityId);
        configStub.setupStubForEidasEnabledForTransaction(rpEntityId, false);
        eventSinkStub.setupStubForLogging();
    }

    @After
    public void after() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldReturnOkWhenASuccessMatchingServiceResponseIsReceived() throws Exception {
        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);

        SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.MatchingServiceMatch,
                        translatedAuthnRequest.getId(),
                        msaEntityId,
                        Optional.of("assertionBlob"),
                        Optional.of(LEVEL_2));
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        Response response = postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(SuccessfulMatchState.class.getName());
    }

    @Test
    public void shouldReturnOkWhenAMatchingServiceFailureResponseIsReceived() throws Exception {
        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.MATCHING_SERVICE_REQUEST_FAILURE_RESOURCE).build(sessionId);
        Response response = postResponse(policy.uri(uri.toASCIIString()), null);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(MatchingServiceRequestErrorState.class.getName());

        // check that the state has been updated
        uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SHOW_MATCHING_ERROR_PAGE);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    public void responseFromMatchingService_shouldThrowExceptionWhenInResponseToDoesNotMatchFromCycle1MatchRequest() throws Exception {
        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);

        SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.MatchingServiceMatch,
                        "a-different-request-id",
                        msaEntityId,
                        Optional.of("assertionBlob"),
                        Optional.of(LEVEL_2));
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        Response response = postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        // Note that the state does not get updated if there is a StateProcessingValidationException
        assertThat(getSessionStateName(sessionId)).isEqualTo(Cycle0And1MatchRequestSentState.class.getName());

    }

    @Test
    public void responseFromMatchingService_shouldThrowExceptionWhenInResponseToDoesNotMatchFromCycle3MatchRequest() throws Exception {
        SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);
        aCycle3AttributeHasBeenSentToPolicyFromTheUser(sessionId, "#1");

        SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.MatchingServiceMatch,
                        "a-thoroughly-different-request-id",
                        msaEntityId,
                        Optional.of("assertionBlob"),
                        Optional.of(LEVEL_2));
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        Response response = postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        // Note that the state does not get updated if there is a StateProcessingValidationException
        assertThat(getSessionStateName(sessionId)).isEqualTo(Cycle3MatchRequestSentState.class.getName());
    }

    @Test
    public void responseProcessingDetails_shouldReturnSuccessResponse_whenNoMatchWithC3Enabled_userAccountCreationAttributesAreFetched() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);
        aCycle3AttributeHasBeenSentToPolicyFromTheUser(sessionId, "#1");
        aNoMatchResponseHasBeenReceivedAndUserAccountCreationIsEnabled(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(UserAccountCreationRequestSentState.class.getName());
    }

    @Test
    public void responseProcessingDetails_shouldReturnSuccessResponse_whenNoMatchWithC3Disabled_userAccountCreationAttributesAreFetched() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Disabled(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(UserAccountCreationRequestSentState.class.getName());
    }

    @Test
    public void fullSuccessfulJourneyThroughAllStates() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);
        aCycle3AttributeHasBeenSentToPolicyFromTheUser(sessionId, "#1");
        aNoMatchResponseHasBeenReceivedAndUserAccountCreationIsEnabled(sessionId);
        aUserAccountCreationResponseIsReceived(sessionId, MatchingServiceIdaStatus.UserAccountCreated);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SEND_USER_ACCOUNT_CREATED_RESPONSE_TO_TRANSACTION);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(UserAccountCreatedState.class.getName());
    }

    @Test
    public void journeyWithFailedAccountCreation() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);
        configStub.setUpStubForEnteringAwaitingCycle3DataState(rpEntityId);
        aCycle3AttributeHasBeenSentToPolicyFromTheUser(sessionId, "#1");
        aNoMatchResponseHasBeenReceivedAndUserAccountCreationIsEnabled(sessionId);
        aUserAccountCreationResponseIsReceived(sessionId, MatchingServiceIdaStatus.UserAccountCreationFailed);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.USER_ACCOUNT_CREATION_FAILED);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(UserAccountCreationFailedState.class.getName());
    }

    @Test
    public void responseProcessingDetails_shouldReturnWaitingForC3Status_whenNoMatchResponseSentFromMatchingServiceAndC3Required() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.GET_C3_DATA);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(AwaitingCycle3DataState.class.getName());
    }

    @Test
    public void isResponseFromHubReady_shouldReturnFailedStatusWhenAProblemHasOccurredWhilstMatchingCycle3() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);
        aCycle3AttributeHasBeenSentToPolicyFromTheUser(sessionId, "#1");
        aMatchingServiceFailureResponseHasBeenReceived(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SHOW_MATCHING_ERROR_PAGE);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(MatchingServiceRequestErrorState.class.getName());
    }

    @Test
    public void isResponseFromHubReady_shouldReturnFailedStatusWhenAProblemHasOccurredWhilstMatchingCycle1() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aMatchingServiceFailureResponseHasBeenReceived(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SHOW_MATCHING_ERROR_PAGE);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(MatchingServiceRequestErrorState.class.getName());
    }

    @Test
    public void getCycle3AttributeRequestData_shouldReturnExpectedAttributeData() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        final String cycle3Attribute = aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_REQUEST_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        final Cycle3AttributeRequestData attributeData = response.readEntity(Cycle3AttributeRequestData.class);
        assertThat(attributeData.getAttributeName()).isEqualTo(cycle3Attribute);
        assertThat(attributeData.getRequestIssuerId()).isEqualTo(rpEntityId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(AwaitingCycle3DataState.class.getName());
    }

    @Test
    public void isResponseFromHubReady_shouldThrowExceptionWhenCycle3MatchingServiceWaitPeriodHasBeenExceeded() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(sessionId);
        aCycle3AttributeHasBeenSentToPolicyFromTheUser(sessionId, "#1");
        theMatchingServiceResponseTimeoutHasBeenExceeded();

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SHOW_MATCHING_ERROR_PAGE);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(MatchingServiceRequestErrorState.class.getName());
    }

    @Test
    public void isResponseFromHubReady_shouldThrowExceptionWhenMatchingServiceWaitPeriodHasBeenExceeded() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        theMatchingServiceResponseTimeoutHasBeenExceeded();

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SHOW_MATCHING_ERROR_PAGE);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(MatchingServiceRequestErrorState.class.getName());
    }

    @Test
    public void isResponseFromHubReady_shouldTellFrontendToShowErrorPageWhenMSRespondsButSamlEngineThrowsInvalidSamlError() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        anIdpIsSelectedForRegistration(sessionId, idpEntityId);
        anIdpAuthnRequestWasGenerated(sessionId);
        anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(sessionId);
        samlEngineRespondsToATranslateAttributeQueryWithAnErrorStatusDto(sessionId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_RESOURCE).build(sessionId);
        Response response = getResponse(policy.uri(uri.toASCIIString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseProcessingDetails responseProcessingDetails = response.readEntity(ResponseProcessingDetails.class);
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SHOW_MATCHING_ERROR_PAGE);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
        assertThat(getSessionStateName(sessionId)).isEqualTo(MatchingServiceRequestErrorState.class.getName());
    }

    private Response getResponse(URI uri) {
        return client
                .target(uri)
                .request()
                .get();
    }

    private Response postResponse(URI uri, Object msaSamlResponseDto) {
        return client
                .target(uri.toASCIIString())
                .request()
                .post(Entity.json(msaSamlResponseDto));
    }

    private String getSessionStateName(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + GET_SESSION_STATE_NAME).build(sessionId);

        final Response response = getResponse(policy.uri(uri.toASCIIString()));
        return response.readEntity(String.class);

    }

    private void samlEngineRespondsToATranslateAttributeQueryWithAnErrorStatusDto(SessionId sessionId) throws JsonProcessingException {
        final SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        samlEngineStub.setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto.createUnauditedErrorStatus(UUID.randomUUID(), ExceptionType.INVALID_SAML));
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        Response response = postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    private void aUserAccountCreationResponseIsReceived(SessionId sessionId, MatchingServiceIdaStatus status) throws JsonProcessingException {
        SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(status,
                        translatedAuthnRequest.getId(),
                        msaEntityId,
                        Optional.of("assertionBlob"),
                        Optional.of(LEVEL_2));
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);
    }

    private void theMatchingServiceResponseTimeoutHasBeenExceeded() {
        DateTimeFreezer.freezeTime(DateTime.now().plusSeconds(matchingServiceResponseWaitPeriodSeconds + 1));
    }

    private void aMatchingServiceFailureResponseHasBeenReceived(SessionId sessionId) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.MATCHING_SERVICE_REQUEST_FAILURE_RESOURCE).build(sessionId);
        postResponse(policy.uri(uri.toASCIIString()), null);
    }

    private void aNoMatchResponseHasBeenReceivedAndUserAccountCreationIsEnabled(SessionId sessionId) throws JsonProcessingException {
        SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                        translatedAuthnRequest.getId(),
                        msaEntityId,
                        Optional.absent(),
                        Optional.absent());
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);
        List<UserAccountCreationAttribute> userAccountCreationAttributes = ImmutableList.of(UserAccountCreationAttribute.CURRENT_ADDRESS);
        configStub.setUpStubForUserAccountCreation(rpEntityId, userAccountCreationAttributes);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);
    }

    private void aCycle3AttributeHasBeenSentToPolicyFromTheUser(SessionId sessionId, String cycle3Attribute) {
        Cycle3UserInput cycle3UserInput = new Cycle3UserInput(cycle3Attribute, "principalIpAsSeenByHub");
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_SUBMIT_RESOURCE).build(sessionId);
        postResponse(policy.uri(uri.toASCIIString()), cycle3UserInput);
    }

    private String aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Enabled(SessionId sessionId) throws JsonProcessingException {
        SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                        translatedAuthnRequest.getId(),
                        msaEntityId,
                        Optional.absent(),
                        Optional.absent());
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);
        final String cycle3Attribute = configStub.setUpStubForEnteringAwaitingCycle3DataState(rpEntityId);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);

        return cycle3Attribute;
    }

    private void aNoMatchResponseWasReceivedFromTheMSAForCycle01_withCycle3Disabled(SessionId sessionId) throws JsonProcessingException {
        SamlResponseDto msaSamlResponseDto = new SamlResponseDto("a-saml-response");
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                        translatedAuthnRequest.getId(),
                        msaEntityId,
                        Optional.absent(),
                        Optional.absent());
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);

        configStub.setUpStubForCycle01NoMatchCycle3Disabled(rpEntityId);

        List<UserAccountCreationAttribute> userAccountCreationAttributes = ImmutableList.of(UserAccountCreationAttribute.CURRENT_ADDRESS);
        configStub.setUpStubForUserAccountCreation(rpEntityId, userAccountCreationAttributes);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        postResponse(policy.uri(uri.toASCIIString()), msaSamlResponseDto);
    }

    private void anIdpAuthnRequestWasGenerated(SessionId sessionId) throws JsonProcessingException {
        final SamlRequestDto samlRequestDto = new SamlRequestDto("coffee-pasta", idpSsoUri);

        samlEngineStub.setupStubForIdpAuthnRequestGenerate(samlRequestDto);

        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_REQUEST_RESOURCE).build(sessionId);
        getResponse(policy.uri(uri.toASCIIString()));
    }

    private void anAuthnResponseFromIdpWasReceivedAndMatchingRequestSent(SessionId sessionId) throws JsonProcessingException {
        final URI policyUri = policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId).getPath());

        SamlAuthnResponseContainerDto samlAuthnResponseContainerDto = SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto()
                                                                                                        .withSamlResponse("saml-response")
                                                                                                        .withSessionId(new SessionId(sessionId.getSessionId()))
                                                                                                        .withPrincipalIPAddressAsSeenByHub("principal-ip-address")
                                                                                                        .withAnalyticsSessionId("this-is-an-analytics-session-id")
                                                                                                        .withJourneyType("this-is-a-journey-type")
                                                                                                        .build();

        InboundResponseFromIdpDto inboundResponseFromIdpDto = InboundResponseFromIdpDtoBuilder.successResponse(idpEntityId, LEVEL_2);
        configStub.setUpStubForMatchingServiceRequest(rpEntityId, msaEntityId);
        samlEngineStub.setupStubForAttributeQueryRequest(AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto().build());
        samlEngineStub.setupStubForIdpAuthnResponseTranslate(inboundResponseFromIdpDto);
        samlSoapProxyProxyStubRule.setUpStubForSendHubMatchingServiceRequest(sessionId);

        postResponse(policyUri, samlAuthnResponseContainerDto);
    }

    private void anIdpIsSelectedForRegistration(SessionId sessionId, String idpEntityId) {
        final URI policyUri = policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE).build(sessionId).getPath());
        postResponse(policyUri, new IdpSelected(idpEntityId, "this-is-an-ip-address", REGISTERING, LEVEL_2, "this-is-an-analytics-session-id", "this-is-a-journey-type"));
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(rpEntityId);
        samlEngineStub.setupStubForAuthnRequestTranslate(translatedAuthnRequest);
        return createASession(rpSamlRequest).readEntity(SessionId.class);
    }

    private Response createASession(SamlAuthnRequestContainerDto samlRequest) {
        return postResponse(policy.uri(Urls.PolicyUrls.NEW_SESSION_RESOURCE), samlRequest);
    }
}
