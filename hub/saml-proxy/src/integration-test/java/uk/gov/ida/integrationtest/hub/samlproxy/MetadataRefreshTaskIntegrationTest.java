package uk.gov.ida.integrationtest.hub.samlproxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataRefreshTaskIntegrationTest {

    @RegisterExtension
    public static final SamlProxyAppExtension samlProxyApp = SamlProxyAppExtension.builder()
            .build();

    private SamlProxyAppExtension.SamlProxyClient client;

    @BeforeEach
    public void beforeEach() {
        client = samlProxyApp.getClient();
    }

    @Test
    public void verifyFederationMetadataRefreshTaskWorks() {
        final Response response = client.postTargetAdmin("/tasks/metadata-refresh", "refresh!");
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

}
