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
import java.util.Collection;

import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.CountryConfigBuilder.aCountryConfig;

public class CountriesResourceIntegrationTest {
    private static Client client;
    private static final String COUNTRY_ID = "AA";
    private static final String SSO_URL = "http://AA.eidas/Eidas/SSO";

    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule()
            .addCountry(
                aCountryConfig()
                    .withEntityId(COUNTRY_ID)
                    .withSimpleId(COUNTRY_ID)
                    .withOverriddenSsoUrl(SSO_URL)
                    .build()
            );

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
}
