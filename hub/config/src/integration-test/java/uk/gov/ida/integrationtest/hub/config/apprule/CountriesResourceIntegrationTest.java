package uk.gov.ida.integrationtest.hub.config.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.domain.CountryConfig;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collection;

import static io.dropwizard.testing.ConfigOverride.config;
import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.CountryConfigBuilder.aCountryConfig;

public class CountriesResourceIntegrationTest {
    private static Client client;
    private static final String COUNTRY_ID = "AA";
    private static final String SSO_URL = "http://AA.eidas/Eidas/SSO";

    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule(
            config("server.applicationConnectors[0].port", "0"),
            config("server.adminConnectors[0].port", "0"),
            config("eidasDisabledAfter", "2020-03-23T10:00:00")
    )
            .addCountry(
                aCountryConfig()
                    .withEntityId(COUNTRY_ID)
                    .withSimpleId(COUNTRY_ID)
                    .withOverriddenSsoUrl(SSO_URL)
                    .build()
            );

    @ClassRule
    public static ConfigAppRule configAppRuleNoEidasTimestamp = new ConfigAppRule(
            config("server.applicationConnectors[0].port", "0"),
            config("server.adminConnectors[0].port", "0"),
            config("eidasDisabledAfter", ""));

    @ClassRule
    public static ConfigAppRule configAppRuleNoEidasExitConfig = new ConfigAppRule(
            "config-no-eidas-exit-timestamp.yml",
            config("server.applicationConnectors[0].port", "0"),
            config("server.adminConnectors[0].port", "0"));

    @BeforeClass
    public static void setUp() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(CountriesResourceIntegrationTest.class.getSimpleName());
    }

    @Test
    public void getShouldReturnCountriesForEidas() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.COUNTRIES_ROOT).build();
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        Collection<CountryConfig> result = response.readEntity(new GenericType<Collection<CountryConfig>>() {}) ;
        assertNotNull(result);
        assertThat(result.size()).isGreaterThan(0);
        CountryConfig countryConfigEntityData = result.stream()
                .filter(countriesConfigEntityData -> countriesConfigEntityData.getSimpleId().equals(COUNTRY_ID))
                .findAny().get();
        assertThat(countryConfigEntityData.getEntityId()).isEqualTo(COUNTRY_ID);
        assertThat(countryConfigEntityData.getOverriddenSsoUrl()).isEqualTo(SSO_URL);
    }

    @Test
    public void getShouldReturnEidasDisabledAfterTimestamp() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.EIDAS_DISABLED_AFTER_RESOURCE).build();
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(LocalDateTime.class)).isEqualTo(configAppRule.getConfiguration().getEidasDisabledAfter().get());
    }

    @Test
    public void getShouldReturnNullWhenEidasDisabledAfterTimestampIsEmpty() {
        URI uri = configAppRuleNoEidasTimestamp.getUri(Urls.ConfigUrls.EIDAS_DISABLED_AFTER_RESOURCE).build();
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        assertThat(response.readEntity(LocalDateTime.class)).isNull();
    }

    @Test
    public void getShouldReturnEidasDisabledAfterTimeStampNotPresent() {
        URI uri = configAppRuleNoEidasExitConfig.getUri(Urls.ConfigUrls.EIDAS_DISABLED_AFTER_RESOURCE).build();
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        assertThat(response.readEntity(LocalDateTime.class)).isNull();
    }
}
