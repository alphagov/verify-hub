package uk.gov.ida.integrationtest.hub.policy.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRuleWithRedis;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

public class EidasDisabledIntegrationTest {

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();
    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();
    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();
    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyProxyStub = new SamlSoapProxyProxyStubRule();
    @ClassRule
    public static PolicyAppRuleWithRedis policy = new PolicyAppRuleWithRedis(
        ConfigOverride.config("eidas", "false"),
        ConfigOverride.config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("samlSoapProxyUri", samlSoapProxyProxyStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()));
    private static Client client;
    private static final String RP_ENTITY_ID = "rpEntityId";

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration
            jerseyClientConfiguration =
            JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client =
            new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(EidasDisabledIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        configStub.reset();
        configStub.setUpStubForLevelsOfAssurance(RP_ENTITY_ID);
        configStub.setupStubForEidasEnabledForTransaction(RP_ENTITY_ID, false);
        eventSinkStub.setupStubForLogging();
    }

    @Test
    public void sessionResourceShouldReturnNotFound() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();

        Response selectedCountryResponse = selectACountry(sessionId);

        assertThat(selectedCountryResponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    public void countriesResourceShouldReturnNotFound() {
        URI uri = policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE).build().toASCIIString());

        Response response = client.target(uri).request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    private Response selectACountry(SessionId sessionId) {
        return TestSessionResourceHelper.selectCountryInSession(
            sessionId,
            client,
            policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE)
                .path(Urls.PolicyUrls.COUNTRY_SET_PATH)
                .build(sessionId, "NL").toString())
        );
    }
}
