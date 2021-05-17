package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule;

import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.samlsoapproxy.SamlSoapProxyApplication;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppExtension;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataRefreshTaskIntegrationTest {

    private static ClientSupport client;

    @RegisterExtension
    public static TestDropwizardAppExtension samlSoapProxyAppExtension = SamlSoapProxyAppExtension.forApp(SamlSoapProxyApplication.class)
            .withDefaultConfigOverridesAnd()
            .config(ResourceHelpers.resourceFilePath("saml-soap-proxy.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
    }

    @Test
    public void verifyFederationMetadataRefreshTaskWorks() {
        final Response response = client.targetAdmin("/tasks/metadata-refresh")
                .request().post(Entity.text("refresh!"));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }
}
