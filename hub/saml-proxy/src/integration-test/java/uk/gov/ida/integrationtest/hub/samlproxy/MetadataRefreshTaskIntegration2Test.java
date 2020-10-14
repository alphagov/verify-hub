package uk.gov.ida.integrationtest.hub.samlproxy;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
public class MetadataRefreshTaskIntegration2Test {

    private static SamlProxyAppExtension EXT = SamlProxyAppExtension.getInstance(false);

    @Test
    public void verifyFederationMetadataRefreshTaskWorks() throws Exception {
        EXT.beforeEach(null);
        final Response response = EXT.client().target(UriBuilder.fromUri("http://localhost")
                .path("/tasks/metadata-refresh")
                .port(EXT.getAdminPort())
                .build())
                .request()
                .post(Entity.text("refresh!"));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        EXT.afterEach(null);

    }

    @Test
    public void eidasConnectorMetadataRefreshTaskWorks() throws Exception {
        EXT.beforeEach(null);
        final Response response = EXT.client().target(UriBuilder.fromUri("http://localhost")
                .path("/tasks/eidas-metadata-refresh")
                .port(EXT.getAdminPort())
                .build())
                .request()
                .post(Entity.text("refresh!"));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        EXT.afterEach(null);
    }
}
