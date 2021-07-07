package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppExtension.SamlSoapProxyClient;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataRefreshTaskIntegrationTest {

    @RegisterExtension
    public static final SamlSoapProxyAppExtension samlSoapProxyApp = SamlSoapProxyAppExtension.builder()
            .build();

    public SamlSoapProxyClient client;

    @BeforeEach
    public void beforeEach() {
        client = samlSoapProxyApp.getClient();
    }

    @Test
    public void verifyFederationMetadataRefreshTaskWorks() {
        final Response response = client.postTargetAdmin("/tasks/metadata-refresh", "refresh!");
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }
}
