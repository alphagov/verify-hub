package uk.gov.ida.integrationtest.hub.samlproxy;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataRefreshTaskIntegrationTest {
    
    private static Client client;

    @ClassRule
    public static SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule();

    @BeforeClass
    public static void setUpClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlProxyAppRule.getEnvironment()).using(jerseyClientConfiguration).build(MetadataRefreshTaskIntegrationTest.class.getSimpleName());
    }

    @Test
    public void verifyFederationMetadataRefreshTaskWorks() {
        final Response response = client.target(UriBuilder.fromUri("http://localhost")
                .path("/tasks/metadata-refresh")
                .port(samlProxyAppRule.getAdminPort())
                .build())
                .request()
                .post(Entity.text("refresh!"));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void eidasConnectorMetadataRefreshTaskWorks() {
        final Response response = client.target(UriBuilder.fromUri("http://localhost")
                .path("/tasks/eidas-metadata-refresh")
                .port(samlProxyAppRule.getAdminPort())
                .build())
                .request()
                .post(Entity.text("refresh!"));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }
}
