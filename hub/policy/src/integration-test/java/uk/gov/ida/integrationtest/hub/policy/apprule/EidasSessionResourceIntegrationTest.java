package uk.gov.ida.integrationtest.hub.policy.apprule;

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
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.domain.CountryAuthenticationStatus;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.OTHER;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.EIDAS_AUTHN_FAILED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.EIDAS_COUNTRY_SELECTED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.builders.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;

public class EidasSessionResourceIntegrationTest {

    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";

    private InboundResponseFromCountry translationDto;
    private AttributeQueryContainerDto aqrDto;
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

    private static final String RP_ENTITY_ID = "rpEntityId";
    private static final String MS_ENTITY_ID = "Matching-service-entity-id";
    private static final EidasCountryDto NETHERLANDS = new EidasCountryDto("http://netherlandsEnitity.nl", "NL", true);
    private static final EidasCountryDto SPAIN = new EidasCountryDto("http://spainEnitity.es", "ES", true);
    private static final List<EidasCountryDto> EIDAS_COUNTRIES = List.of(NETHERLANDS, SPAIN);

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment())
                .using(jerseyClientConfiguration)
                .build(EidasSessionResourceIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        stubSamlEngineTranslationLOAForCountry(LevelOfAssurance.LEVEL_2, NETHERLANDS);
        stubSamlEngineGenerationOfAQR();
        configStub.reset();
        configStub.setUpStubForMatchingServiceRequest(RP_ENTITY_ID, MS_ENTITY_ID, true);
        configStub.setUpStubForLevelsOfAssurance(RP_ENTITY_ID);
        configStub.setupStubForEidasEnabledForTransaction(RP_ENTITY_ID, false);
        configStub.setupStubForEidasCountries(EIDAS_COUNTRIES);
        eventSinkStub.reset();
        eventSinkStub.setupStubForLogging();
        enableCountriesForRp(RP_ENTITY_ID, NETHERLANDS, SPAIN);
    }

    @Test
    public void shouldReturnOkWhenSuccessAuthnResponseIsReceived() throws Exception {
        SessionId sessionId = selectACountry(NETHERLANDS);
        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);

        Response response = postAuthnResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ResponseAction expectedResult = ResponseAction.success(sessionId, false, LevelOfAssurance.LEVEL_2, null);
        assertThat(response.readEntity(ResponseAction.class)).isEqualToComparingFieldByField(expectedResult);

        assertThatCurrentStateForSesssionIs(sessionId, EidasCycle0And1MatchRequestSentState.class);
    }

    @Test
    public void shouldFailWhenSessionIsInvalid() {
        SessionId sessionId = SessionId.createNewSessionId();

        Response response = postAuthnResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldFailWhenCountryIsDisabledWhileInSession() throws Exception {
        enableCountriesForRp(RP_ENTITY_ID, SPAIN);
        SessionId sessionId = selectACountry(NETHERLANDS);

        Response response = postAuthnResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldFailWhenLOAIsNotWhatWasRequested() throws Exception {
        stubSamlEngineTranslationLOAForCountry(LevelOfAssurance.LEVEL_1, NETHERLANDS);
        SessionId sessionId = selectACountry(NETHERLANDS);

        Response response = postAuthnResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnPackagedFailureResponseWhenSessionInEidasAuthnFailedState() {
        final SessionId sessionId = createSessionInEidasAuthnFailedState();

        final Response response = postAuthnResponseToPolicy(sessionId);
        final ResponseAction responseAction = response.readEntity(ResponseAction.class);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(responseAction.getResult()).isEqualTo(OTHER);
    }

    @Test
    public void shouldReturnPackagedFailureResponseWhenTranslationFails() throws Exception {
        stubSamlEngineTranslationToFailForCountry(NETHERLANDS);
        SessionId sessionId = selectACountry(NETHERLANDS);

        Response response = postAuthnResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        ResponseAction responseAction = response.readEntity(ResponseAction.class);

        assertThat(responseAction.getResult()).isEqualTo(OTHER);
    }

    @Test
    public void shouldFailWhenTranslationDoesNotReturn2XX() throws Exception {
        stubSamlEngineTranslationToReturnBadRequest();
        SessionId sessionId = selectACountry(NETHERLANDS);

        Response response = postAuthnResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private void assertThatCurrentStateForSesssionIs(SessionId sessionId, Class state) {
        policy.getSessionState(sessionId, state);
    }

    private SessionId createSessionInEidasCountrySelectingState() {
        SessionId sessionId = SessionId.createNewSessionId();
        URI uri = policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + EIDAS_COUNTRY_SELECTED_STATE).build().toASCIIString());
        TestSessionResourceHelper.createSessionInEidasCountrySelectingState(
                sessionId,
                client,
                uri,
                RP_ENTITY_ID,
                true);
        return sessionId;
    }

    private SessionId createSessionInEidasAuthnFailedState() {
        SessionId sessionId = SessionId.createNewSessionId();
        URI uri = policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + EIDAS_AUTHN_FAILED_STATE).build().toASCIIString());
        TestSessionResourceHelper.createSessionInEidasAuthnFailedErrorState(sessionId, client, uri);
        return sessionId;
    }

    private Response postAuthnResponseToPolicy(SessionId sessionId) {
        URI countryResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.COUNTRY_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        return client
                .target(policy.uri(countryResponseUri.toASCIIString()))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(aSamlAuthnResponseContainerDto().withSessionId(sessionId).build()));
    }

    private SessionId selectACountry(EidasCountryDto dto) {
        SessionId sessionId = createSessionInEidasCountrySelectingState();
        TestSessionResourceHelper.selectCountryInSession(
                sessionId,
                client,
                policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE)
                        .path(Urls.PolicyUrls.COUNTRY_SET_PATH)
                        .build(sessionId, dto.getSimpleId()).toString())
        );
        return sessionId;
    }

    private void stubSamlEngineTranslationLOAForCountry(LevelOfAssurance loa, EidasCountryDto country) throws Exception {
        samlEngineStub.reset();
        translationDto = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Success,
                Optional.empty(),
                country.getEntityId(),
                Optional.of("BLOB"),
                Optional.of("PID"),
                Optional.of(loa),
                Optional.empty(),
                Optional.empty()
        );
        samlEngineStub.setupStubForCountryAuthnResponseTranslate(translationDto);
    }

    private void stubSamlEngineTranslationToFailForCountry(EidasCountryDto country) throws Exception {
        samlEngineStub.reset();
        translationDto = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Failure,
                Optional.empty(),
                country.getEntityId(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        samlEngineStub.setupStubForCountryAuthnResponseTranslate(translationDto);
    }

    private void stubSamlEngineTranslationToReturnBadRequest() throws Exception {
        samlEngineStub.reset();
        samlEngineStub.setupStubForCountryAuthnResponseTranslationFailure();
    }

    private void stubSamlEngineGenerationOfAQR() throws Exception {
        aqrDto = new AttributeQueryContainerDto("SAML", URI.create("/foo"), "id", DateTime.now(), "issuer", true);
        samlEngineStub.setupStubForEidasAttributeQueryRequestGeneration(aqrDto);
    }

    private void enableCountriesForRp(String rpEntityId, EidasCountryDto... countries) throws Exception {
        configStub.setupStubForEidasRPCountries(rpEntityId, Arrays.stream(countries).map(EidasCountryDto::getEntityId).collect(toList()));
    }
}
