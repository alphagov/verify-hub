package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceNotFound404IntegrationTests {

    private static Client client;

    @ClassRule
    public static SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule();

    @BeforeClass
    public static void setUpClass() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlProxyAppRule.getEnvironment()).using(jerseyClientConfiguration).build(SamlMessageReceiverApiResourceTest.class.getSimpleName());
    }

    @Test
    public void samlProxyService_shouldReturn404WhenInvalidUrlAccessed(){
        Response response = client
                .target(samlProxyAppRule.getUri("/this-page-does-not-exist"))
                .request()
                .get(Response.class);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }
}
