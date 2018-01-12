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
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.domain.*;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.COUNTRY_SELECTED_STATE;
import static uk.gov.ida.integrationtest.hub.policy.builders.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;

public class EidasSessionResourceIntegrationTest {

    public static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";

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
    private static final ImmutableList<EidasCountryDto> EIDAS_COUNTRIES = ImmutableList.of(NETHERLANDS, SPAIN);

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
        ResponseAction expectedResult = ResponseAction.success(sessionId, false, LevelOfAssurance.LEVEL_2);
        assertThat(response.readEntity(ResponseAction.class)).isEqualToComparingFieldByField(expectedResult);

        assertThatCurrentStateForSesssionIs(sessionId, EidasCycle0And1MatchRequestSentState.class);
    }

    @Test
    public void shouldFailWhenSessionIsInvalid() throws Exception {
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

        // TODO: Bug in CEF Reference 1.1. Remove this try..catch when EID-177 CEF Reference 1.3 is deployed.
//        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldFailWhenTranslationFails() throws Exception {
        stubSamlEngineTranslationToFailForCountry(NETHERLANDS);
        SessionId sessionId = selectACountry(NETHERLANDS);

        Response response = postAuthnResponseToPolicy(sessionId);

        // TODO: Bug in CEF Reference 1.1. Remove this try..catch when EID-177 CEF Reference 1.3 is deployed.
//        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldFailWhenTranslationDoesNotReturn2XX() throws Exception {
        stubSamlEngineTranslationToReturnBadRequest();
        SessionId sessionId = selectACountry(NETHERLANDS);

        Response response = postAuthnResponseToPolicy(sessionId);

        // TODO: Bug in CEF Reference 1.1. Remove this try..catch when EID-177 CEF Reference 1.3 is deployed.
//        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private void assertThatCurrentStateForSesssionIs(SessionId sessionId, Class state) {
        policy.getSessionState(sessionId, state);
    }

    private SessionId createSessionInCountrySelectingState() {
        SessionId sessionId = SessionId.createNewSessionId();
        URI uri = policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + COUNTRY_SELECTED_STATE).build().toASCIIString());
        TestSessionResourceHelper.createSessionInCountrySelectingState(
                sessionId,
                client,
                uri,
                RP_ENTITY_ID,
                true);
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
        SessionId sessionId = createSessionInCountrySelectingState();
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
        translationDto = new InboundResponseFromCountry(IdpIdaStatus.Status.Success, Optional.absent(), country.getEntityId(), Optional.of("BLOB"), Optional.of("PID"), Optional.of(loa));
        samlEngineStub.setupStubForCountryAuthnResponseTranslate(translationDto);
    }

    private void stubSamlEngineTranslationToFailForCountry(EidasCountryDto country) throws Exception {
        samlEngineStub.reset();
        translationDto = new InboundResponseFromCountry(IdpIdaStatus.Status.RequesterError, Optional.absent(), country.getEntityId(), Optional.absent(), Optional.absent(), Optional.absent());
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
