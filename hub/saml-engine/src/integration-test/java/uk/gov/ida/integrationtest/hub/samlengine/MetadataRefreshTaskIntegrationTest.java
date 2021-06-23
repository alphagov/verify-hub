package uk.gov.ida.integrationtest.hub.samlengine;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineDropwizardAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineDropwizardAppExtension.SamlEngineAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineDropwizardAppExtension.SamlEngineClient;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataRefreshTaskIntegrationTest {

    @RegisterExtension
    public static SamlEngineDropwizardAppExtension samlEngineApp = new SamlEngineAppExtensionBuilder().build();

    private SamlEngineClient client;

    @BeforeEach
    public void beforeEach() throws Exception {
        client = samlEngineApp.getClient();
    }

    @AfterAll
    public static void afterAll() {
        samlEngineApp.tearDown();
    }

    @Test
    public void verifyFederationMetadataRefreshTaskWorks() {
        final Response response = client.postTargetAdmin("/tasks/metadata-refresh", "refresh!");
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

}
