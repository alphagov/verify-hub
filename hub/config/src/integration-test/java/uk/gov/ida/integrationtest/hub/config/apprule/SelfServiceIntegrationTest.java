package uk.gov.ida.integrationtest.hub.config.apprule;

import com.adobe.testing.s3mock.junit4.S3MockRule;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.dto.MatchingServiceConfigDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

public class SelfServiceIntegrationTest {

    public static Client client;
    private static final String ENTITY_ID = "test-ms-entity-id";
    private static final String MATCHING_URI = "http://foo.bar/matching-service-uri";
    private static final String BUCKET_NAME = "s3Bucket";
    private static final String OBJECT_KEY = "src/test/resources/remote-test-config.json";

    @ClassRule(order = 1)
    public static S3MockRule s3MockRule = S3MockRule.builder().silent().build();

    @ClassRule(order = 2)
    public static ConfigAppRule configAppRule = new ConfigAppRule(s3MockRule::createS3Client, getSelfServiceOverrides())
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
                    .withOnboarding(asList("rp-entity-id"))
                    .build());

    private static ConfigOverride[] getSelfServiceOverrides() {
        ConfigOverride[] overrides = {
                ConfigOverride.config("selfService.enabled", "true"),
                ConfigOverride.config("selfService.s3BucketName", BUCKET_NAME),
                ConfigOverride.config("selfService.s3ObjectKey", OBJECT_KEY),
                ConfigOverride.config("selfService.cacheExpiry", "5s")
        };
        return overrides;
    }


    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(SelfServiceIntegrationTest.class.getSimpleName());
    }

    @Test
    public void getMatchingServices_returnsOkAndListOfMatchingServices(){
        Response response = client.target(configAppRule.getUri(Urls.ConfigUrls.MATCHING_SERVICE_ROOT).build()).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class).size()).isEqualTo(1);
    }

    @Test
    public void getMatchingService_returnsMatchingServiceForEntityId(){
        String entityId = ENTITY_ID;
        URI uri = configAppRule.getUri(Urls.ConfigUrls.MATCHING_SERVICE_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));

        Response response = client.target(uri.toASCIIString()).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        MatchingServiceConfigDto ms = response.readEntity(MatchingServiceConfigDto.class);
        assertThat(ms.getEntityId()).isEqualTo(entityId);
        assertThat(ms.getUri().toString()).isEqualTo(MATCHING_URI);
    }

    @Test
    public void getMatchingService_returnsInternalServerError(){
        String entityId = "not-found";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.MATCHING_SERVICE_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri.toASCIIString()).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(response.readEntity(ErrorMessage.class).getMessage()).contains("There was an error processing your request.");
    }
}
