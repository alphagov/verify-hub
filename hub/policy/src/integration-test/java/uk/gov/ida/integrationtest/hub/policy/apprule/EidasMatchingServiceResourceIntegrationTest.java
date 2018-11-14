package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.CountryAuthenticationStatus;
import uk.gov.ida.hub.policy.domain.Cycle3UserInput;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchingServiceIdaStatus;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.EIDAS_CYCLE_0_1_MATCHING_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper.createSessionInEidasCycle01MatchRequestSentState;

public class EidasMatchingServiceResourceIntegrationTest {
    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyProxyStub = new SamlSoapProxyProxyStubRule();

    @ClassRule
    public static PolicyAppRule policy = new PolicyAppRule(
        ConfigOverride.config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("samlSoapProxyUri", samlSoapProxyProxyStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("eidas", "true"));

    private static Client client;
    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";
    private static URI cycle01Uri;
    private static final String MSA_ENTITY_ID = "msaEntityId";
    private static final String RP_ENTITY_ID = "rpEntityId";
    private static final EidasCountryDto NETHERLANDS = new EidasCountryDto("http://netherlandsEnitity.nl", "NL", true);
    private static final EidasCountryDto SPAIN = new EidasCountryDto("http://spainEnitity.es", "ES", true);
    private static final ImmutableList<EidasCountryDto> EIDAS_COUNTRIES = ImmutableList.of(NETHERLANDS, SPAIN);

    private SamlResponseWithAuthnRequestInformationDto translatedAuthnRequest;
    private SamlAuthnRequestContainerDto rpSamlRequest;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment())
            .using(jerseyClientConfiguration)
            .build(EidasMatchingServiceResourceIntegrationTest.class.getSimpleName());

        cycle01Uri = policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + EIDAS_CYCLE_0_1_MATCHING_STATE).build().toASCIIString());
    }

    @Before
    public void setUp() throws Exception {
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(RP_ENTITY_ID).build();
        rpSamlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        configStub.reset();
        configStub.setupStubForEidasCountries(EIDAS_COUNTRIES);
        configStub.setUpStubForMatchingServiceRequest(RP_ENTITY_ID, MSA_ENTITY_ID, true);
        configStub.setupStubForEidasEnabledForTransaction(RP_ENTITY_ID, true);
        configStub.setUpStubForLevelsOfAssurance(RP_ENTITY_ID);
        enableCountriesForRp(RP_ENTITY_ID, NETHERLANDS, SPAIN);

        eventSinkStub.setupStubForLogging();

        stubSamlEngineTranslationLOAForCountry(LevelOfAssurance.LEVEL_2, NETHERLANDS);
        stubSamlEngineGenerationOfAQR();
    }

    @Test
    public void shouldTransitionToEidasSuccessfulMatchStateWhenMatchIsReceivedForEidasCycle0And1() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlEngineStub.setupStubForAttributeResponseTranslate(aMatchResponse());

        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasSuccessfulMatchState.class.getName());
    }

    @Test
    public void shouldTransitionToEidasAwaitingCycle3DataStateWhenNoMatchIsReceivedForEidasCycle0And1WithCycle3Enabled() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        configStub.setUpStubForEnteringAwaitingCycle3DataState(RP_ENTITY_ID);

        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasAwaitingCycle3DataState.class.getName());
    }

    @Test
    public void shouldTransitionToEidasUserAccountCreationRequestSentStateWhenNoMatchIsReceivedForEidasCycle0And1WithCycle3DisabledAndUACEnabled() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = new InboundResponseFromMatchingServiceDto(
                MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                translatedAuthnRequest.getId(),
                MSA_ENTITY_ID,
                Optional.absent(),
                Optional.absent());
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);
        configStub.setUpStubForCycle01NoMatchCycle3Disabled(RP_ENTITY_ID);
        configStub.setUpStubForUserAccountCreation(RP_ENTITY_ID, Collections.singletonList(UserAccountCreationAttribute.FIRST_NAME));

        final Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasUserAccountCreationRequestSentState.class.getName());
    }

    @Test
    public void shouldTransitionToNoMatchStateWhenNoMatchIsReceivedForEidasCycle0And1WithCycle3DisabledAndUACDisabled() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        configStub.setUpStubForCycle01NoMatchCycle3Disabled(RP_ENTITY_ID);
        configStub.setUpStubForUserAccountCreation(RP_ENTITY_ID, Collections.emptyList());

        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(NoMatchState.class.getName());
    }

    @Test
    public void shouldTransitionToEidasSuccessfulMatchStateWhenMatchIsReceivedForEidasCycle3() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        configStub.setUpStubForEnteringAwaitingCycle3DataState(RP_ENTITY_ID);

        postAttributeQueryResponseToPolicy(sessionId);
        postCycle3Data(sessionId, new Cycle3UserInput("test-value", "principal-ip-address-seen-by-hub"));

        samlEngineStub.setupStubForAttributeResponseTranslate(aMatchResponse());
        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasSuccessfulMatchState.class.getName());
    }

    @Test
    public void shouldTransitionToEidasUserAccountCreationRequestSentStateWhenNoMatchIsReceivedForEidasCycle3WhenUACEnabled() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        configStub.setUpStubForEnteringAwaitingCycle3DataState(RP_ENTITY_ID);
        configStub.setUpStubForUserAccountCreation(RP_ENTITY_ID, Collections.singletonList(UserAccountCreationAttribute.FIRST_NAME));

        postAttributeQueryResponseToPolicy(sessionId);
        postCycle3Data(sessionId, new Cycle3UserInput("test-value", "principal-ip-address-seen-by-hub"));

        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasUserAccountCreationRequestSentState.class.getName());
    }

    @Test
    public void shouldTransitionToNoMatchStateWhenNoMatchIsReceivedForEidasCycle3WhenUACDisabled() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        configStub.setUpStubForEnteringAwaitingCycle3DataState(RP_ENTITY_ID);
        configStub.setUpStubForUserAccountCreation(RP_ENTITY_ID, Collections.emptyList());

        postAttributeQueryResponseToPolicy(sessionId);
        postCycle3Data(sessionId, new Cycle3UserInput("test-value", "principal-ip-address-seen-by-hub"));

        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(NoMatchState.class.getName());
    }

    @Test
    public void shouldTransitionToEidasUserAccountCreationFailedStateWhenUACFailedResponseIsReceivedForEidasCycle3WhenUACEnabled() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        configStub.setUpStubForEnteringAwaitingCycle3DataState(RP_ENTITY_ID);
        configStub.setUpStubForUserAccountCreation(RP_ENTITY_ID, Collections.singletonList(UserAccountCreationAttribute.FIRST_NAME));

        postAttributeQueryResponseToPolicy(sessionId);
        postCycle3Data(sessionId, new Cycle3UserInput("test-value", "principal-ip-address-seen-by-hub"));

        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        postAttributeQueryResponseToPolicy(sessionId);

        samlEngineStub.setupStubForAttributeResponseTranslate(aUserAccountCreationFailedResponse());
        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasUserAccountCreationFailedState.class.getName());
    }

    @Test
    public void shouldTransitionToUserAccountCreatedStateWhenUACreatedResponseIsReceivedForEidasCycle3WhenUACEnabled() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        createSessionInEidasCycle01MatchRequestSentState(sessionId, translatedAuthnRequest.getId(), RP_ENTITY_ID, MSA_ENTITY_ID, client, cycle01Uri);

        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        configStub.setUpStubForEnteringAwaitingCycle3DataState(RP_ENTITY_ID);
        configStub.setUpStubForUserAccountCreation(RP_ENTITY_ID, Collections.singletonList(UserAccountCreationAttribute.FIRST_NAME));

        postAttributeQueryResponseToPolicy(sessionId);
        postCycle3Data(sessionId, new Cycle3UserInput("test-value", "principal-ip-address-seen-by-hub"));

        samlEngineStub.setupStubForAttributeResponseTranslate(aNoMatchResponse());
        postAttributeQueryResponseToPolicy(sessionId);

        samlEngineStub.setupStubForAttributeResponseTranslate(aUserAccountCreatedResponse());
        Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(UserAccountCreatedState.class.getName());
    }

    private InboundResponseFromMatchingServiceDto aNoMatchResponse(){
        return new InboundResponseFromMatchingServiceDto(
                MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                translatedAuthnRequest.getId(),
                MSA_ENTITY_ID,
                Optional.absent(),
                Optional.absent());
    }

    private InboundResponseFromMatchingServiceDto aMatchResponse(){
        return new InboundResponseFromMatchingServiceDto(
                MatchingServiceIdaStatus.MatchingServiceMatch,
                translatedAuthnRequest.getId(),
                MSA_ENTITY_ID,
                Optional.of("assertionBlob"),
                Optional.of(LevelOfAssurance.LEVEL_2));
    }

    private InboundResponseFromMatchingServiceDto aUserAccountCreatedResponse(){
        return new InboundResponseFromMatchingServiceDto(
                MatchingServiceIdaStatus.UserAccountCreated,
                translatedAuthnRequest.getId(),
                MSA_ENTITY_ID,
                Optional.of("assertionBlob"),
                Optional.of(LevelOfAssurance.LEVEL_2));
    }

    private InboundResponseFromMatchingServiceDto aUserAccountCreationFailedResponse(){
        return new InboundResponseFromMatchingServiceDto(
                MatchingServiceIdaStatus.UserAccountCreationFailed,
                translatedAuthnRequest.getId(),
                MSA_ENTITY_ID,
                Optional.of("assertionBlob"),
                Optional.of(LevelOfAssurance.LEVEL_2));
    }

    private Response postAttributeQueryResponseToPolicy(final SessionId sessionId) {
        URI attributeQueryResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        return postResponse(attributeQueryResponseUri, new SamlResponseDto("a-saml-response"));
    }

    private Response postCycle3Data(final SessionId sessionId, final Cycle3UserInput data) {
        URI uri = UriBuilder.fromPath(Urls.PolicyUrls.CYCLE_3_SUBMIT_RESOURCE).build(sessionId);
        return postResponse(uri, data);
    }

    private Response postResponse(URI uri, Object payload) {
        return client
                .target(policy.uri(uri.toASCIIString()))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(payload));
    }

    private String getSessionStateName(final SessionId sessionId) {
        final URI sessionStateNameUri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + GET_SESSION_STATE_NAME).build(sessionId);
        return client
            .target(policy.uri(sessionStateNameUri.toASCIIString()))
            .request()
            .get().readEntity(String.class);
    }

    private void enableCountriesForRp(final String rpEntityId, final EidasCountryDto... countries) throws Exception {
        configStub.setupStubForEidasRPCountries(rpEntityId, Arrays.stream(countries).map(EidasCountryDto::getEntityId).collect(toList()));
    }

    private void stubSamlEngineTranslationLOAForCountry(final LevelOfAssurance loa, final EidasCountryDto country) throws Exception {
        samlEngineStub.reset();
        InboundResponseFromCountry translationDto = new InboundResponseFromCountry(CountryAuthenticationStatus.Status.Success, Optional.absent(), country.getEntityId(), Optional.of("BLOB"), Optional.of("PID"), Optional.of(loa));
        samlEngineStub.setupStubForCountryAuthnResponseTranslate(translationDto);
    }

    private void stubSamlEngineGenerationOfAQR() throws Exception {
        AttributeQueryContainerDto aqrDto = new AttributeQueryContainerDto("SAML", URI.create("/foo"), "id", DateTime.now(), "issuer", true);
        samlEngineStub.setupStubForEidasAttributeQueryRequestGeneration(aqrDto);
    }
}
