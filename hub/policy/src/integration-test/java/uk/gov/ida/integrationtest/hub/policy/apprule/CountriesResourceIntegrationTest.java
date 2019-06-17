package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.EIDAS_COUNTRY_SELECTED_STATE;

public class CountriesResourceIntegrationTest {

    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";

    private static Client client;
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

    private static final String RP_ENTITY_ID = "rpEntityId";
    private SessionId sessionId;

    private static final EidasCountryDto NETHERLANDS = new EidasCountryDto("http://netherlandsEnitity.nl", "NL", true);
    private static final EidasCountryDto SPAIN = new EidasCountryDto("http://spainEnitity.es", "ES", true);
    private static final EidasCountryDto FRANCE = new EidasCountryDto("http://franceEnitity.fr", "FR", true);
    private static final EidasCountryDto FRANCE_DISABLED = new EidasCountryDto("http://franceEnitity.fr", "FR", false);

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(CountriesResourceIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        eventSinkStub.setupStubForLogging();
        sessionId = SessionId.createNewSessionId();
        createSessionInEidasCountrySelectingState(sessionId, true);
    }

    @After
    public void resetStubs() {
        eventSinkStub.reset();
        configStub.reset();
    }

    @Test
    public void shouldReturnCountriesWhenEidasJourneyIsEnabled() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS, SPAIN);

        List<EidasCountryDto> configuredCountries = listCountriesForSession();

        assertThat(configuredCountries).isEqualTo(ImmutableList.of(NETHERLANDS, SPAIN));
    }

    @Test
    public void shouldReturnOnlyEnabledCountriesWhenEidasJourneyIsEnabled() throws Exception {
        setEidasCountries(NETHERLANDS, FRANCE_DISABLED);
        setEidasCountriesForRp(NETHERLANDS, FRANCE_DISABLED);

        List<EidasCountryDto> configuredCountries = listCountriesForSession();

        assertThat(configuredCountries).isEqualTo(ImmutableList.of(NETHERLANDS));
    }

    @Test
    public void shouldReturnOnlyEnabledCountriesForRpWhenEidasJourneyIsEnabled() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS);

        List<EidasCountryDto> configuredCountries = listCountriesForSession();

        assertThat(configuredCountries).isEqualTo(ImmutableList.of(NETHERLANDS));
    }

    @Test
    public void shouldReturnAllEnabledCountriesWhenNoCountriesConfiguredForRp() throws JsonProcessingException {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp();

        List<EidasCountryDto> configuredCountries = listCountriesForSession();

        assertThat(configuredCountries).isEqualTo(ImmutableList.of(NETHERLANDS, SPAIN));
    }

    @Test
    public void shouldReturnErrorWhenRequestingAListOfCountriesWithoutExistingSession() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS, SPAIN);
        sessionId = SessionId.createNewSessionId();

        Response response = requestListOfCountriesForSession();

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldSelectCountryWhenEidasJourneyIsEnabled() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS, SPAIN);

        Response response = selectCountry(NETHERLANDS);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        String recordedEvent = new String (eventSinkStub.getLastRequest().getEntityBytes());
        assertThat(recordedEvent).contains(sessionId.getSessionId());
        assertThat(recordedEvent).contains(EventSinkHubEventConstants.SessionEvents.COUNTRY_SELECTED);
        assertThat(recordedEvent).contains(NETHERLANDS.getEntityId());
    }

    @Test
    public void shouldReturnBadRequestWhenWrongCountryIsSelected() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS, SPAIN);

        Response response = selectCountry(FRANCE_DISABLED);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnBadRequestWhenCountryIsSelectedAndSessionDoesNotSupportEidas() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS, SPAIN);
        createSessionInEidasCountrySelectingState(sessionId, false);

        Response response = selectCountry(NETHERLANDS);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldBeAbleToSelectAnotherCountryWhenEidasJourneyIsEnabled() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS, SPAIN);

        selectCountry(NETHERLANDS);
        Response response = selectCountry(SPAIN);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

        String recordedEvent = new String(eventSinkStub.getLastRequest().getEntityBytes());
        assertThat(recordedEvent).contains(sessionId.getSessionId());
        assertThat(recordedEvent).contains(EventSinkHubEventConstants.SessionEvents.COUNTRY_SELECTED);
        assertThat(recordedEvent).contains(SPAIN.getEntityId());
    }

    @Test
    public void shouldNotBeAbleToSelectCountryWhichIsNotEnabled() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN, FRANCE_DISABLED);
        setEidasCountriesForRp(NETHERLANDS, SPAIN);

        Response response = selectCountry(FRANCE_DISABLED);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldNotBeAbleToSelectCountryWhichIsNotEnabledForRp() throws Exception {
        setEidasCountries(NETHERLANDS, SPAIN);
        setEidasCountriesForRp(NETHERLANDS);

        Response response = selectCountry(SPAIN);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private void createSessionInEidasCountrySelectingState(SessionId sessionId, boolean transactionSupportsEidas) {
        URI uri = policy.uri(UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + EIDAS_COUNTRY_SELECTED_STATE).build().toASCIIString());
        Response sessionCreatedResponse = TestSessionResourceHelper.createSessionInEidasCountrySelectingState(
                sessionId,
                client,
                uri,
                RP_ENTITY_ID,
                transactionSupportsEidas);
        assertThat(sessionCreatedResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    private List<EidasCountryDto> listCountriesForSession() {
        return getEntity(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE)
                .path(Urls.SharedUrls.SESSION_ID_PARAM_PATH)
                .build(sessionId), new GenericType<List<EidasCountryDto>>() {});
    }

    private Response requestListOfCountriesForSession() {
        return get(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE)
                .path(Urls.SharedUrls.SESSION_ID_PARAM_PATH)
                .build(sessionId));
    }

    private Response selectCountry(EidasCountryDto country) {
        return TestSessionResourceHelper.selectCountryInSession(
                sessionId,
                client,
                policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE)
                        .path(Urls.PolicyUrls.COUNTRY_SET_PATH)
                        .build(sessionId, country.getSimpleId()).toString())
        );

    }

    private void setEidasCountries(EidasCountryDto... countries) throws JsonProcessingException {
        configStub.setupStubForEidasCountries(Arrays.asList(countries));
    }

    private void setEidasCountriesForRp(EidasCountryDto... countries) throws JsonProcessingException {
        configStub.setupStubForEidasRPCountries(RP_ENTITY_ID, Arrays.stream(countries).map(EidasCountryDto::getEntityId).collect(toList()));
    }

    private <T> T getEntity(URI uri, GenericType<T> type) {
        Response response = get(uri);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        return get(uri).readEntity(type);
    }

    private Response get(URI uri) {
        final URI uri1 = policy.uri(uri.toASCIIString());
        return client.target(uri1).request(MediaType.APPLICATION_JSON_TYPE).get();
    }
}
