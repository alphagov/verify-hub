package uk.gov.ida.integrationtest.hub.config.apprule;

import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.dto.MatchingServiceConfigDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.ConfigClient;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.ConfigAppExtensionBuilder;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

@ExtendWith(DropwizardExtensionsSupport.class)
public class MatchingServiceResourceIntegrationTest {
    private static final String ENTITY_ID = "test-ms-entity-id";
    private static final String MATCHING_URI = "http://foo.bar/matching-service-uri";

    private static final ConfigAppExtension app = ConfigAppExtensionBuilder.forApp(ConfigApplication.class)
            .addTransaction(aTransactionConfigData()
                    .withEntityId("rp-entity-id")
                    .withMatchingServiceEntityId(ENTITY_ID)
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withEntityId("rp-entity-id-no-matching")
                    .withUsingMatching(false)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(ENTITY_ID)
                    .withUri(URI.create(MATCHING_URI))
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId("idp-entity-id")
                    .withOnboarding(singletonList("rp-entity-id"))
                    .build())
            .build();

    private ConfigClient client;

    @BeforeEach
    void setup() { client = app.getClient(); }

    @AfterAll
    static void tearDown() { app.tearDown(); }

    @Test
    public void getMatchingServices_returnsOkAndListOfMatchingServices(){
        Response response = client.targetMain(Urls.ConfigUrls.MATCHING_SERVICE_ROOT);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class).size()).isEqualTo(1);
    }

    @Test
    public void getMatchingService_returnsMatchingServiceForEntityId(){
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.MATCHING_SERVICE_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        MatchingServiceConfigDto ms = response.readEntity(MatchingServiceConfigDto.class);
        assertThat(ms.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(ms.getUri().toString()).isEqualTo(MATCHING_URI);
    }

    @Test
    public void getMatchingService_returnsInternalServerError(){
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.MATCHING_SERVICE_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(response.readEntity(ErrorMessage.class).getMessage()).contains("There was an error processing your request.");
    }

    private Response getForEntityIdAndPath(String entityId, String path) {
        URI uri = UriBuilder.fromPath(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.targetMain(uri);
    }
}
