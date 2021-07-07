package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension.SamlProxyClient;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceNotFound404IntegrationTests {

    @RegisterExtension
    public static final SamlProxyAppExtension samlProxyApp = SamlProxyAppExtension.builder()
            .build();

    private SamlProxyClient client;

    @BeforeEach
    public void beforeEach() {
        client = samlProxyApp.getClient();
    }

    @AfterAll
    public static void tearDown() {
        samlProxyApp.tearDown();
    }

    @Test
    public void samlProxyService_shouldReturn404WhenInvalidUrlAccessed(){
        Response response = client.getTargetMain("/this-page-does-not-exist");
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }
}
